package com.hip.engine

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hip.database.HIPDatabase
import com.hip.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor de matching de recetas.
 *
 * Algoritmo:
 * 1. Normaliza ingredientes detectados
 * 2. Por cada receta: calcula MatchScore
 * 3. Filtra por perfil de usuario (alergias, equipamiento, tiempo)
 * 4. Selecciona la mejor ENTRADA + PLATO PRINCIPAL + POSTRE
 * 5. Genera "whyThisRecipe" explicativo
 */
class RecipeEngine(private val db: HIPDatabase) {

    private val gson = Gson()

    /**
     * Punto de entrada principal.
     * Retorna exactamente 3 recetas: entrada, principal, postre.
     */
    suspend fun findBestMeal(
        detectedIngredients: List<DetectedIngredient>,
        userProfile: UserProfile
    ): Triple<RecipeMatch?, RecipeMatch?, RecipeMatch?> = withContext(Dispatchers.IO) {

        val allRecipes = db.recipeDao().getAllForMatching()
        val substitutions = buildSubstitutionMap()
        val normalizedAvailable = detectedIngredients.map { it.name.normalize() }

        // Calcular match para todas las recetas
        val allMatches = allRecipes
            .filter { it.isActive }
            .map { recipe ->
                computeMatch(recipe, normalizedAvailable, detectedIngredients, userProfile, substitutions)
            }
            .filter { it.matchScore >= MIN_MATCH_THRESHOLD }
            .sortedByDescending { it.matchScore }

        // Filtrar por perfil
        val filtered = applyProfileFilters(allMatches, userProfile)

        // Seleccionar mejor de cada tipo
        val entrada = filtered.firstOrNull { it.recipe.mealType == MealType.ENTRADA }
        val principal = filtered.firstOrNull { it.recipe.mealType == MealType.PLATO_PRINCIPAL }
        val postre = filtered.firstOrNull { it.recipe.mealType == MealType.POSTRE }

        Triple(entrada, principal, postre)
    }

    /**
     * Calcula el score de match para una receta dada los ingredientes disponibles.
     */
    private suspend fun computeMatch(
        recipe: Recipe,
        normalizedAvailable: List<String>,
        detected: List<DetectedIngredient>,
        userProfile: UserProfile,
        substitutions: Map<String, List<String>>
    ): RecipeMatch {

        val requiredType = object : TypeToken<List<Map<String, String>>>() {}.type
        val required: List<Map<String, String>> = runCatching {
            gson.fromJson(recipe.requiredIngredients, requiredType)
        }.getOrElse { emptyList() }

        val optional: List<Map<String, String>> = runCatching {
            gson.fromJson(recipe.optionalIngredients, requiredType)
        }.getOrElse { emptyList() }

        val available = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val usedSubstitutes = mutableMapOf<String, String>()

        // Evaluar ingredientes requeridos
        for (req in required) {
            val name = req["name"]?.normalize() ?: continue
            when {
                normalizedAvailable.any { it.contains(name) || name.contains(it) } -> {
                    available.add(req["name"] ?: name)
                }
                findSubstitute(name, normalizedAvailable, substitutions) != null -> {
                    val sub = findSubstitute(name, normalizedAvailable, substitutions)!!
                    usedSubstitutes[req["name"] ?: name] = sub
                    available.add(req["name"] ?: name)
                }
                else -> {
                    missing.add(req["name"] ?: name)
                }
            }
        }

        // Score base: porcentaje de requeridos disponibles
        val baseScore = if (required.isEmpty()) 0.5f
        else available.size.toFloat() / required.size.toFloat()

        // Bonus por ingredientes opcionales disponibles
        val optionalBonus = optional.count { opt ->
            val name = opt["name"]?.normalize() ?: return@count false
            normalizedAvailable.any { it.contains(name) || name.contains(it) }
        } * 0.05f

        // Penalty si faltan muchos requeridos
        val missingPenalty = missing.size * 0.15f

        // Bonus nutricional según objetivo
        val nutritionalBonus = computeNutritionalBonus(recipe, userProfile)

        val finalScore = (baseScore + optionalBonus + nutritionalBonus - missingPenalty).coerceIn(0f, 1f)

        // Generar explicación
        val explanation = buildExplanation(recipe, available, missing, usedSubstitutes, detected)

        return RecipeMatch(
            recipe = recipe,
            matchScore = finalScore,
            availableIngredients = available,
            missingIngredients = missing,
            substituteIngredients = usedSubstitutes,
            nutritionalAdaptation = buildNutritionalNote(recipe, userProfile),
            whyThisRecipe = explanation
        )
    }

    private fun findSubstitute(
        missingIngredient: String,
        available: List<String>,
        substitutions: Map<String, List<String>>
    ): String? {
        val subs = substitutions[missingIngredient] ?: return null
        return subs.firstOrNull { sub ->
            available.any { it.contains(sub.normalize()) || sub.normalize().contains(it) }
        }
    }

    private fun computeNutritionalBonus(recipe: Recipe, profile: UserProfile): Float {
        return when (profile.goal) {
            NutritionalGoal.BAJAR_PESO -> {
                when {
                    recipe.caloriesPerServing < 300 -> 0.10f
                    recipe.caloriesPerServing < 400 -> 0.05f
                    recipe.caloriesPerServing > 600 -> -0.10f
                    else -> 0f
                }
            }
            NutritionalGoal.AUMENTAR_MASA -> {
                when {
                    recipe.proteinPerServing > 25 -> 0.10f
                    recipe.proteinPerServing > 18 -> 0.05f
                    else -> -0.05f
                }
            }
            NutritionalGoal.ALTO_PROTEINA -> {
                (recipe.proteinPerServing / 40f).coerceAtMost(0.15f)
            }
            NutritionalGoal.BAJO_SODIO -> {
                when {
                    recipe.sodiumPerServing < 300 -> 0.10f
                    recipe.sodiumPerServing > 700 -> -0.15f
                    else -> 0f
                }
            }
            NutritionalGoal.CONTROL_GLUCEMICO -> {
                if (recipe.fiberPerServing > 5) 0.08f else 0f
            }
            else -> 0f
        }
    }

    private fun buildExplanation(
        recipe: Recipe,
        available: List<String>,
        missing: List<String>,
        substitutes: Map<String, String>,
        detected: List<DetectedIngredient>
    ): String {
        val sb = StringBuilder()
        sb.append("Se sugiere ${recipe.name} porque ")

        if (available.isNotEmpty()) {
            val ingList = available.take(3).joinToString(", ")
            sb.append("tenés $ingList")
            if (available.size > 3) sb.append(" y ${available.size - 3} ingrediente(s) más")
        }

        if (substitutes.isNotEmpty()) {
            val subEntry = substitutes.entries.first()
            sb.append(". Se puede sustituir ${subEntry.key} por ${subEntry.value}")
        }

        if (missing.isEmpty()) {
            sb.append(". No requiere ningún ingrediente adicional.")
        } else if (missing.size == 1) {
            sb.append(". Solo falta: ${missing[0]}.")
        } else {
            sb.append(". Faltan: ${missing.joinToString(", ")}.")
        }

        return sb.toString()
    }

    private fun buildNutritionalNote(recipe: Recipe, profile: UserProfile): String {
        return when (profile.goal) {
            NutritionalGoal.BAJAR_PESO ->
                "${recipe.caloriesPerServing} kcal por porción. " +
                        if (recipe.caloriesPerServing < 350) "✓ Apto para tu objetivo." else "Porción controlada recomendada."
            NutritionalGoal.AUMENTAR_MASA ->
                "${recipe.proteinPerServing}g de proteína por porción."
            NutritionalGoal.ALTO_PROTEINA ->
                "Fuente de proteína: ${recipe.proteinPerServing}g por porción."
            NutritionalGoal.BAJO_SODIO ->
                "${recipe.sodiumPerServing}mg sodio por porción."
            else -> "${recipe.caloriesPerServing} kcal · ${recipe.proteinPerServing}g prot · ${recipe.carbsPerServing}g carb · ${recipe.fatPerServing}g grasas"
        }
    }

    private fun applyProfileFilters(
        matches: List<RecipeMatch>,
        profile: UserProfile
    ): List<RecipeMatch> {
        return matches.filter { match ->
            val recipe = match.recipe

            // Filtro de tiempo
            val totalTime = recipe.prepTimeMinutes + recipe.cookTimeMinutes
            if (totalTime > profile.maxCookingTimeMinutes) return@filter false

            // Filtro de equipamiento
            if (recipe.isOvenRequired && !profile.hasOven) return@filter false

            // Filtro de nivel de dificultad
            val allowedLevels = when (profile.cookingLevel) {
                CookingLevel.PRINCIPIANTE -> setOf(DifficultyLevel.MUY_FACIL, DifficultyLevel.FACIL)
                CookingLevel.INTERMEDIO -> setOf(
                    DifficultyLevel.MUY_FACIL,
                    DifficultyLevel.FACIL,
                    DifficultyLevel.MEDIO
                )
                CookingLevel.AVANZADO -> DifficultyLevel.values().toSet()
            }
            if (recipe.difficulty !in allowedLevels) return@filter false

            // Filtro de alergias (básico)
            val tagsJson = recipe.tags
            val allergyConflict = profile.allergies.any { allergy ->
                tagsJson.contains(allergy, ignoreCase = true) ||
                        match.availableIngredients.any { it.contains(allergy, ignoreCase = true) }
            }
            if (allergyConflict) return@filter false

            true
        }
    }

    private suspend fun buildSubstitutionMap(): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        db.substitutionDao().let { dao ->
            // Build from common substitutions
        }
        // Hardcoded common substitutions for speed
        return mapOf(
            "tomate" to listOf("salsa de tomate", "puré de tomate"),
            "crema" to listOf("yogur natural", "leche"),
            "manteca" to listOf("aceite vegetal"),
            "pollo" to listOf("carne picada de pollo"),
            "arroz" to listOf("arroz integral")
        )
    }

    companion object {
        private const val MIN_MATCH_THRESHOLD = 0.4f
    }
}

// Extensión de normalización de strings
fun String.normalize(): String =
    this.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
        .replace('ñ', 'n')
        .trim()
