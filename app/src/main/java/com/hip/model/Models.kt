package com.hip.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.hip.database.Converters

// ─────────────────────────────────────────────────────
//  INGREDIENTE
// ─────────────────────────────────────────────────────

enum class IngredientState {
    CRUDO, COCIDO, CONGELADO, PICADO, ENTERO, EN_BARRA, RALLADO,
    EN_FETAS, EN_SALSA, ABIERTO, CERRADO, DESCONOCIDO
}

enum class IngredientCategory {
    CARNES, LACTEOS, VERDURAS, FRUTAS, CEREALES, LEGUMBRES,
    CONDIMENTOS, ACEITES, HUEVOS, BEBIDAS, CONGELADOS, OTROS
}

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,   // para matching (lowercase, sin acentos)
    val category: IngredientCategory,
    val aliases: String = "",     // JSON array de nombres alternativos
    val defaultUnit: String = "unidad",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val fiberPer100g: Double = 0.0,
    val sodiumPer100g: Double = 0.0,
    val glycemicIndex: Int = 50,
    val isCommon: Boolean = true  // ingrediente común en cocina uruguaya
)

// Ingrediente detectado durante el escaneo (no persistido en DB de ingredientes)
data class DetectedIngredient(
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val state: IngredientState = IngredientState.DESCONOCIDO,
    val confidence: Float = 0f,
    val isManuallyAdded: Boolean = false,
    val isConfirmedByUser: Boolean = false,
    val ingredientId: Long = -1,
    val estimatedGrams: Double = 0.0
)

// ─────────────────────────────────────────────────────
//  RECETA
// ─────────────────────────────────────────────────────

enum class MealType {
    ENTRADA, PLATO_PRINCIPAL, POSTRE, MERIENDA, DESAYUNO
}

enum class DifficultyLevel {
    MUY_FACIL, FACIL, MEDIO, DIFICIL
}

enum class RecipeCategory {
    SOPAS_CALDOS, CARNES, PASTA_ARROZ, ENSALADAS, TARTAS_TORTILLAS,
    POSTRES, GUISOS, MILANESAS, SANDWICHES, DESAYUNOS, OTROS
}

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val mealType: MealType,
    val category: RecipeCategory,
    val difficulty: DifficultyLevel,

    // Ingredientes como JSON
    val requiredIngredients: String,   // JSON: List<RecipeIngredient>
    val optionalIngredients: String,   // JSON: List<RecipeIngredient>
    val substitutions: String,         // JSON: Map<String, List<String>>

    // Instrucciones
    val steps: String,                 // JSON: List<String>

    // Tiempo
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,

    // Utensilios mínimos
    val requiredEquipment: String,     // JSON: List<String>

    // Costo estimado (UYU, zona metro Montevideo)
    val estimatedCostUYU: Int,

    // Porciones
    val servings: Int = 4,

    // Nutricional por porción
    val caloriesPerServing: Int = 0,
    val proteinPerServing: Double = 0.0,
    val carbsPerServing: Double = 0.0,
    val fatPerServing: Double = 0.0,
    val fiberPerServing: Double = 0.0,
    val sodiumPerServing: Double = 0.0,
    val nutritionalScore: Int = 0,     // 1-10

    // Tags para filtrado
    val tags: String = "",             // JSON: List<String>

    // Modos de cocina soportados
    val isOvenRequired: Boolean = false,
    val isMicrowaveOk: Boolean = false,
    val isFreezerFriendly: Boolean = false,

    // Versión (para actualizaciones futuras)
    val version: Int = 1,
    val isActive: Boolean = true
)

// Ingrediente dentro de una receta
data class RecipeIngredient(
    val name: String,
    val normalizedName: String,
    val quantityMin: Double,
    val quantityMax: Double,
    val unit: String,
    val isOptional: Boolean = false,
    val notes: String = ""
)

// ─────────────────────────────────────────────────────
//  PERFIL DE USUARIO
// ─────────────────────────────────────────────────────

enum class NutritionalGoal {
    BAJAR_PESO, MANTENER, AUMENTAR_MASA, CONTROL_GLUCEMICO,
    BAJO_SODIO, ALTO_PROTEINA, NINGUNO
}

enum class CookingLevel {
    PRINCIPIANTE, INTERMEDIO, AVANZADO
}

data class UserProfile(
    val id: String = "default",
    val age: Int = 0,
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val goal: NutritionalGoal = NutritionalGoal.MANTENER,
    val cookingLevel: CookingLevel = CookingLevel.INTERMEDIO,
    val allergies: List<String> = emptyList(),
    val intolerances: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),
    val dailyCalorieTarget: Int = 2000,
    val hasOven: Boolean = true,
    val hasMicrowave: Boolean = true,
    val hasBlender: Boolean = false,
    val hasElectricGrill: Boolean = false,
    val preferQuickMeals: Boolean = false,   // < 30 min
    val maxCookingTimeMinutes: Int = 60,
    val budgetPerMealUYU: Int = 300
)

// ─────────────────────────────────────────────────────
//  RESULTADO DE MATCHING
// ─────────────────────────────────────────────────────

data class RecipeMatch(
    val recipe: Recipe,
    val matchScore: Float,           // 0-1
    val availableIngredients: List<String>,
    val missingIngredients: List<String>,
    val substituteIngredients: Map<String, String>,
    val nutritionalAdaptation: String,
    val whyThisRecipe: String,       // "Se sugiere X porque tienes Y y Z..."
    val adaptedSteps: List<String> = emptyList()
)

// Sesión de escaneo (no persistida en DB)
data class ScanSession(
    val photoGeneral: String? = null,
    val photoTop: String? = null,
    val photoBottom: String? = null,
    val photoDrawer: String? = null,
    val detectedIngredients: MutableList<DetectedIngredient> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────
//  SUSTITUCIONES
// ─────────────────────────────────────────────────────

@Entity(tableName = "substitutions")
data class Substitution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalIngredient: String,
    val substituteIngredient: String,
    val ratio: Double = 1.0,
    val notes: String = "",
    val affectsFlavor: Boolean = false,
    val affectsTexture: Boolean = false
)
