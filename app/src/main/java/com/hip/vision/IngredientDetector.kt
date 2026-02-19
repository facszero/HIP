package com.hip.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hip.model.DetectedIngredient
import com.hip.model.IngredientCategory
import com.hip.model.IngredientState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper sobre ML Kit para detección de ingredientes.
 *
 * Estrategia:
 * 1. Detección de objetos (bounding boxes)
 * 2. Etiquetado de imagen (qué hay en la foto)
 * 3. Mapeo de etiquetas ML Kit → ingredientes conocidos
 * 4. Retorna ingredientes con confianza y bounding boxes
 */
class IngredientDetector {

    // Detector de objetos (bounding boxes)
    private val objectDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    // Labeler de imagen general
    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
        ImageLabeling.getClient(options)
    }

    /**
     * Analiza un bitmap y retorna ingredientes detectados.
     */
    suspend fun detectIngredients(bitmap: Bitmap): List<DetectedIngredientResult> {
        val image = InputImage.fromBitmap(bitmap, 0)

        // Ejecutar ambos detectores en paralelo
        val labels = runLabeler(image)
        val objects = runObjectDetector(image)

        // Combinar resultados
        val fromLabels = labels
            .mapNotNull { label ->
                mapLabelToIngredient(label.text, label.confidence)
            }

        val fromObjects = objects.flatMap { detected ->
            detected.labels.mapNotNull { label ->
                mapLabelToIngredient(
                    label.text,
                    label.confidence,
                    detected.boundingBox
                )
            }
        }

        // Merge y deduplicar
        return (fromLabels + fromObjects)
            .groupBy { it.normalizedName }
            .map { (_, group) ->
                // Tomar el de mayor confianza
                group.maxByOrNull { it.confidence }!!
            }
            .sortedByDescending { it.confidence }
    }

    private suspend fun runLabeler(image: InputImage) = suspendCancellableCoroutine { cont ->
        imageLabeler.process(image)
            .addOnSuccessListener { labels -> cont.resume(labels) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    private suspend fun runObjectDetector(image: InputImage) =
        suspendCancellableCoroutine { cont ->
            objectDetector.process(image)
                .addOnSuccessListener { objects -> cont.resume(objects) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    /**
     * Mapea etiquetas de ML Kit a ingredientes reconocidos.
     * Este es el "diccionario de traducción" entre ML Kit y nuestra base de datos.
     *
     * ML Kit detecta categorías genéricas (Food, Fruit, Vegetable) no específicas.
     * Por eso usamos un mapa extendido con términos en inglés/español.
     */
    private fun mapLabelToIngredient(
        label: String,
        confidence: Float,
        boundingBox: Rect? = null
    ): DetectedIngredientResult? {

        val normalLabel = label.lowercase().trim()

        val mapping = INGREDIENT_MAPPING.entries.firstOrNull { (key, _) ->
            normalLabel.contains(key) || key.contains(normalLabel)
        } ?: return null

        return DetectedIngredientResult(
            displayName = mapping.value.displayName,
            normalizedName = mapping.value.normalizedName,
            category = mapping.value.category,
            confidence = confidence,
            boundingBox = boundingBox,
            suggestedState = IngredientState.DESCONOCIDO // El usuario confirma el estado
        )
    }

    fun release() {
        objectDetector.close()
        imageLabeler.close()
    }

    companion object {
        // Mapa: etiqueta ML Kit (inglés) → ingrediente HIP
        private val INGREDIENT_MAPPING = mapOf(
            "egg" to IngredientInfo("Huevo", "huevo", IngredientCategory.HUEVOS),
            "chicken" to IngredientInfo("Pollo", "pollo", IngredientCategory.CARNES),
            "meat" to IngredientInfo("Carne", "carne", IngredientCategory.CARNES),
            "beef" to IngredientInfo("Carne vacuna", "carne", IngredientCategory.CARNES),
            "tomato" to IngredientInfo("Tomate", "tomate", IngredientCategory.VERDURAS),
            "onion" to IngredientInfo("Cebolla", "cebolla", IngredientCategory.VERDURAS),
            "potato" to IngredientInfo("Papa", "papa", IngredientCategory.VERDURAS),
            "carrot" to IngredientInfo("Zanahoria", "zanahoria", IngredientCategory.VERDURAS),
            "zucchini" to IngredientInfo("Zapallito", "zapallito", IngredientCategory.VERDURAS),
            "pepper" to IngredientInfo("Morrón", "morron", IngredientCategory.VERDURAS),
            "garlic" to IngredientInfo("Ajo", "ajo", IngredientCategory.CONDIMENTOS),
            "lettuce" to IngredientInfo("Lechuga", "lechuga", IngredientCategory.VERDURAS),
            "spinach" to IngredientInfo("Espinaca", "espinaca", IngredientCategory.VERDURAS),
            "milk" to IngredientInfo("Leche", "leche", IngredientCategory.LACTEOS),
            "cheese" to IngredientInfo("Queso", "queso mozzarella", IngredientCategory.LACTEOS),
            "butter" to IngredientInfo("Manteca", "manteca", IngredientCategory.LACTEOS),
            "cream" to IngredientInfo("Crema", "crema", IngredientCategory.LACTEOS),
            "ham" to IngredientInfo("Jamón", "jamon", IngredientCategory.CARNES),
            "sausage" to IngredientInfo("Salchicha", "salchicha", IngredientCategory.CARNES),
            "rice" to IngredientInfo("Arroz", "arroz", IngredientCategory.CEREALES),
            "pasta" to IngredientInfo("Fideos", "fideos", IngredientCategory.CEREALES),
            "apple" to IngredientInfo("Manzana", "manzana", IngredientCategory.FRUTAS),
            "banana" to IngredientInfo("Banana", "banana", IngredientCategory.FRUTAS),
            "lemon" to IngredientInfo("Limón", "limon", IngredientCategory.FRUTAS),
            "orange" to IngredientInfo("Naranja", "naranja", IngredientCategory.FRUTAS),
            "lentil" to IngredientInfo("Lentejas", "lentejas", IngredientCategory.LEGUMBRES),
            "bean" to IngredientInfo("Porotos", "porotos", IngredientCategory.LEGUMBRES),
            "oil" to IngredientInfo("Aceite", "aceite", IngredientCategory.ACEITES),
            "flour" to IngredientInfo("Harina", "harina", IngredientCategory.CEREALES),
            // Genéricos
            "vegetable" to IngredientInfo("Verdura (confirmar)", "verdura", IngredientCategory.VERDURAS),
            "fruit" to IngredientInfo("Fruta (confirmar)", "fruta", IngredientCategory.FRUTAS),
            "food" to IngredientInfo("Alimento (confirmar)", "alimento", IngredientCategory.OTROS)
        )
    }
}

// DTOs internos
data class IngredientInfo(
    val displayName: String,
    val normalizedName: String,
    val category: IngredientCategory
)

data class DetectedIngredientResult(
    val displayName: String,
    val normalizedName: String,
    val category: IngredientCategory,
    val confidence: Float,
    val boundingBox: Rect? = null,
    val suggestedState: IngredientState = IngredientState.DESCONOCIDO
)

// Extensión para convertir al modelo del dominio
fun DetectedIngredientResult.toDetectedIngredient() = DetectedIngredient(
    name = displayName,
    state = suggestedState,
    confidence = confidence,
    isManuallyAdded = false,
    isConfirmedByUser = false
)
