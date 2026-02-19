package com.hip.llm

import com.google.gson.Gson
import com.hip.model.DetectedIngredient
import com.hip.model.Recipe
import com.hip.model.RecipeMatch
import com.hip.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Adaptador LLM para ajuste fino de recetas.
 *
 * IMPORTANTE: El LLM NO crea recetas desde cero.
 * Solo adapta la redacción y pequeños detalles de recetas ya seleccionadas
 * por el motor estructurado (RecipeEngine).
 *
 * Prompts cerrados: no puede inventar ingredientes que no estén en el contexto.
 */
class LLMAdapter(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Adapta los pasos de una receta al contexto real del usuario.
     * Respeta privacidad: NO envía fotos, solo texto.
     */
    suspend fun adaptRecipeSteps(
        match: RecipeMatch,
        availableIngredients: List<DetectedIngredient>,
        userProfile: UserProfile
    ): List<String> = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            // Sin API key: devolver pasos originales sin modificar
            return@withContext parseSteps(match.recipe.steps)
        }

        val prompt = buildAdaptationPrompt(match, availableIngredients, userProfile)

        try {
            val response = callLLM(prompt)
            parseAdaptedSteps(response)
        } catch (e: Exception) {
            // Fallback: pasos originales
            parseSteps(match.recipe.steps)
        }
    }

    /**
     * Prompt cerrado y controlado.
     * No permite que el LLM invente ingredientes fuera del contexto.
     */
    private fun buildAdaptationPrompt(
        match: RecipeMatch,
        available: List<DetectedIngredient>,
        profile: UserProfile
    ): String {
        val ingredientList = available.joinToString(", ") {
            "${it.name}${if (it.state.name != "DESCONOCIDO") " (${it.state.name.lowercase()})" else ""}"
        }
        val originalSteps = parseSteps(match.recipe.steps).joinToString("\n") { "- $it" }
        val substitutionNote = if (match.substituteIngredients.isNotEmpty()) {
            "Sustituciones a usar: ${match.substituteIngredients.map { "${it.key} → ${it.value}" }.joinToString(", ")}"
        } else ""

        return """
Sos un asistente de cocina casera uruguaya. Adaptá los siguientes pasos de receta al contexto dado.

REGLAS ESTRICTAS:
1. NO agregues ingredientes que no estén en la lista de disponibles o en la receta original.
2. NO uses términos de cocina sofisticada (no: "reducción", "deglasear", "brunoise", "blanquear").
3. Usá lenguaje cotidiano rioplatense (tuteo, "vos", "che", "olla", "sartén").
4. Si el usuario tiene una sustitución, mencionala naturalmente.
5. Máximo 8 pasos. Cada paso máximo 2 oraciones.
6. Retorná SOLO un JSON array de strings: ["paso 1", "paso 2", ...]
7. NO incluyas explicaciones fuera del JSON.

RECETA: ${match.recipe.name}
INGREDIENTES DISPONIBLES: $ingredientList
$substitutionNote
OBJETIVO NUTRICIONAL: ${profile.goal.name.replace('_', ' ')}
NIVEL DE COCINA: ${profile.cookingLevel.name}

PASOS ORIGINALES A ADAPTAR:
$originalSteps

Retorná el JSON array de pasos adaptados:
        """.trimIndent()
    }

    private suspend fun callLLM(prompt: String): String {
        val requestBody = gson.toJson(mapOf(
            "model" to "gpt-3.5-turbo",
            "messages" to listOf(
                mapOf("role" to "system", "content" to "Sos un asistente de cocina casera uruguaya. Respondé SOLO en JSON."),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 600,
            "temperature" to 0.3  // Bajo para consistencia
        ))

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) throw Exception("LLM error: ${response.code}")

        // Extraer content del response
        val jsonResponse = gson.fromJson(body, Map::class.java)
        val choices = jsonResponse["choices"] as? List<*> ?: throw Exception("No choices")
        val firstChoice = choices.firstOrNull() as? Map<*, *> ?: throw Exception("No first choice")
        val message = firstChoice["message"] as? Map<*, *> ?: throw Exception("No message")
        return message["content"] as? String ?: throw Exception("No content")
    }

    private fun parseAdaptedSteps(json: String): List<String> {
        return try {
            val clean = json.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            gson.fromJson(clean, Array<String>::class.java).toList()
        } catch (e: Exception) {
            // Si el LLM no respetó el formato, extraer líneas
            json.lines()
                .filter { it.isNotBlank() }
                .map { it.removePrefix("-").trim() }
                .filter { it.isNotBlank() }
        }
    }

    private fun parseSteps(stepsJson: String): List<String> {
        return try {
            gson.fromJson(stepsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
