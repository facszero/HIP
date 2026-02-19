package com.hip.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hip.HIPApplication
import com.hip.R
import com.hip.databinding.ActivityRecipeDetailBinding
import com.hip.model.DifficultyLevel
import com.hip.model.NutritionalGoal
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de receta.
 *
 * Muestra:
 * - Nombre y categoría
 * - "Por qué se eligió esta receta" (muy visible)
 * - Adaptación nutricional personalizada
 * - Ingredientes (disponibles / sustituciones / faltantes)
 * - Pasos de preparación
 * - Tabla nutricional completa
 * - Equipamiento necesario
 */
class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val app by lazy { application as HIPApplication }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val match = RecipeSession.current ?: run { finish(); return }
        val recipe = match.recipe

        // Header
        binding.tvRecipeName.text = recipe.name
        binding.tvRecipeDescription.text = recipe.description

        // Por qué esta receta (caja verde destacada)
        binding.tvWhyThisRecipe.text = match.whyThisRecipe
        binding.tvNutritionalAdaptation.text = match.nutritionalAdaptation

        // Badges de info rápida
        val totalTime = recipe.prepTimeMinutes + recipe.cookTimeMinutes
        binding.tvTimeInfo.text = "⏱ $totalTime min"
        binding.tvDifficultyInfo.text = when (recipe.difficulty) {
            DifficultyLevel.MUY_FACIL -> "😊 Muy fácil"
            DifficultyLevel.FACIL -> "👍 Fácil"
            DifficultyLevel.MEDIO -> "🧑‍🍳 Medio"
            DifficultyLevel.DIFICIL -> "👨‍🍳 Difícil"
        }
        binding.tvCostInfo.text = "💰 ~\$${recipe.estimatedCostUYU} UYU"
        binding.tvServingsInfo.text = "🍽 ${recipe.servings} porciones"

        // Equipamiento
        val equipment = runCatching {
            gson.fromJson(recipe.requiredEquipment, Array<String>::class.java).toList()
        }.getOrElse { emptyList() }
        binding.tvEquipment.text = equipment.joinToString(" · ")

        // Ingredientes
        setupIngredientsSections(match.availableIngredients, match.missingIngredients, match.substituteIngredients)

        // Pasos
        loadSteps(recipe.steps)

        // Tabla nutricional
        setupNutrition(recipe, app)

        // Acciones
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShare.setOnClickListener { shareRecipe() }
    }

    private fun setupIngredientsSections(
        available: List<String>,
        missing: List<String>,
        substitutes: Map<String, String>
    ) {
        // Disponibles
        if (available.isNotEmpty()) {
            binding.tvAvailableIngredients.text = available.joinToString("\n") { "✓ $it" }
        } else {
            binding.tvAvailableIngredients.text = "—"
        }

        // Sustituciones
        if (substitutes.isNotEmpty()) {
            binding.cardSubstitutions.visibility = View.VISIBLE
            binding.tvSubstitutions.text = substitutes.entries.joinToString("\n") {
                "• ${it.key} → ${it.value}"
            }
        } else {
            binding.cardSubstitutions.visibility = View.GONE
        }

        // Faltantes
        if (missing.isNotEmpty()) {
            binding.cardMissing.visibility = View.VISIBLE
            binding.tvMissingIngredients.text = missing.joinToString("\n") { "• $it" }
        } else {
            binding.cardMissing.visibility = View.GONE
        }
    }

    private fun loadSteps(stepsJson: String) {
        val steps = runCatching {
            gson.fromJson(stepsJson, Array<String>::class.java).toList()
        }.getOrElse { emptyList() }

        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = StepsAdapter(steps)
    }

    private fun setupNutrition(recipe: com.hip.model.Recipe, app: HIPApplication) {
        binding.tvCalories.text = "${recipe.caloriesPerServing} kcal"
        binding.tvProtein.text = "${recipe.proteinPerServing}g"
        binding.tvCarbs.text = "${recipe.carbsPerServing}g"
        binding.tvFat.text = "${recipe.fatPerServing}g"
        binding.tvFiber.text = "${recipe.fiberPerServing}g"
        binding.tvSodium.text = "${recipe.sodiumPerServing}mg"
        binding.tvNutritionalScore.text = "${recipe.nutritionalScore}/10"

        // Progress bars proporcionales
        val maxCal = 800f
        binding.progressCalories.progress = ((recipe.caloriesPerServing / maxCal) * 100).toInt().coerceIn(0, 100)
        binding.progressProtein.progress = ((recipe.proteinPerServing / 50f) * 100).toInt().coerceIn(0, 100)
        binding.progressCarbs.progress = ((recipe.carbsPerServing / 100f) * 100).toInt().coerceIn(0, 100)
        binding.progressFat.progress = ((recipe.fatPerServing / 40f) * 100).toInt().coerceIn(0, 100)

        // Nota según objetivo
        lifecycleScope.launch {
            val profile = app.preferencesManager.getUserProfile()
            binding.tvGoalNote.text = when (profile.goal) {
                NutritionalGoal.BAJAR_PESO ->
                    if (recipe.caloriesPerServing < 400) "✓ Apto para tu objetivo de bajar peso" else "⚠ Moderá la porción para tu objetivo"
                NutritionalGoal.AUMENTAR_MASA ->
                    if (recipe.proteinPerServing > 20) "✓ Buena fuente de proteína para aumentar masa" else "💡 Sumá una fuente de proteína extra"
                NutritionalGoal.BAJO_SODIO ->
                    if (recipe.sodiumPerServing < 500) "✓ Bajo en sodio" else "⚠ Controlá las porciones por el sodio"
                else -> "Porción para ${recipe.servings} persona(s)"
            }
        }
    }

    private fun shareRecipe() {
        val match = RecipeSession.current ?: return
        val text = buildString {
            appendLine("🍽 ${match.recipe.name}")
            appendLine()
            appendLine(match.whyThisRecipe)
            appendLine()
            appendLine("⏱ ${match.recipe.prepTimeMinutes + match.recipe.cookTimeMinutes} min · 🔥 ${match.recipe.caloriesPerServing} kcal")
            appendLine()
            appendLine("📱 Heladera Inteligente Pro (HIP)")
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir receta"))
    }
}

// ─────────────────────────────────────────────────────
//  ADAPTER DE PASOS
// ─────────────────────────────────────────────────────

class StepsAdapter(private val steps: List<String>) :
    RecyclerView.Adapter<StepsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStepNumber: TextView = view.findViewById(R.id.tvStepNumber)
        val tvStepText: TextView = view.findViewById(R.id.tvStepText)
        val btnCheck: View = view.findViewById(R.id.btnCheckStep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvStepNumber.text = "${position + 1}"
        holder.tvStepText.text = steps[position]

        // Toggle visual de paso completado
        var completed = false
        holder.btnCheck.setOnClickListener {
            completed = !completed
            holder.itemView.alpha = if (completed) 0.5f else 1.0f
            holder.tvStepNumber.text = if (completed) "✓" else "${position + 1}"
        }
    }

    override fun getItemCount() = steps.size
}
