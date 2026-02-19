package com.hip.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.hip.HIPApplication
import com.hip.R
import com.hip.databinding.ActivityRecipeListBinding
import com.hip.model.MealType
import com.hip.model.RecipeMatch
import com.hip.ui.scan.ScanSession
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Muestra las 3 recetas seleccionadas: entrada + principal + postre.
 *
 * Cada receta incluye:
 * - Por qué se sugiere (whyThisRecipe)
 * - Ingredientes disponibles vs faltantes
 * - Info nutricional resumida
 * - Score de compatibilidad
 */
class RecipeListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeListBinding
    private val app by lazy { application as HIPApplication }

    private val mealSet = mutableListOf<RecipeMatch>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadRecipes()
    }

    private fun setupUI() {
        binding.rvRecipes.layoutManager = LinearLayoutManager(this)

        binding.btnScanNew.setOnClickListener {
            // Volver al inicio
            finishAffinity()
        }

        binding.btnEditIngredients.setOnClickListener {
            finish()  // Volver a revisión
        }
    }

    private fun loadRecipes() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.rvRecipes.visibility = View.GONE
        binding.tvLoadingStatus.text = "Buscando las mejores recetas..."

        val confirmedIngredients = ScanSession.current.confirmedIngredients

        lifecycleScope.launch {
            val userProfile = app.preferencesManager.getUserProfile()

            binding.tvLoadingStatus.text = "Calculando compatibilidad nutricional..."

            val (entrada, principal, postre) = app.recipeEngine.findBestMeal(
                confirmedIngredients,
                userProfile
            )

            binding.tvLoadingStatus.text = "Listo!"

            // Construir la lista de 3 recetas
            mealSet.clear()
            entrada?.let { mealSet.add(it) }
            principal?.let { mealSet.add(it) }
            postre?.let { mealSet.add(it) }

            if (mealSet.isEmpty()) {
                binding.progressLoading.visibility = View.GONE
                binding.tvNoRecipes.visibility = View.VISIBLE
                binding.tvNoRecipesHint.visibility = View.VISIBLE
            } else {
                binding.progressLoading.visibility = View.GONE
                binding.rvRecipes.visibility = View.VISIBLE
                binding.tvNoRecipes.visibility = View.GONE

                // Info nutricional del menú completo
                updateNutritionalSummary()

                val adapter = RecipeMatchAdapter(mealSet) { match ->
                    openRecipeDetail(match)
                }
                binding.rvRecipes.adapter = adapter
            }
        }
    }

    private fun updateNutritionalSummary() {
        val totalCalories = mealSet.sumOf { it.recipe.caloriesPerServing }
        val totalProtein = mealSet.sumOf { it.recipe.proteinPerServing }

        binding.tvTotalCalories.text = "${totalCalories} kcal total del menú"
        binding.tvTotalProtein.text = "${totalProtein.roundToInt()}g proteína"

        val avgScore = mealSet.map { it.recipe.nutritionalScore }.average()
        binding.tvNutritionalScore.text = "Puntuación nutricional: ${avgScore.roundToInt()}/10"
    }

    private fun openRecipeDetail(match: RecipeMatch) {
        RecipeSession.current = match
        val intent = Intent(this, RecipeDetailActivity::class.java)
        startActivity(intent)
    }
}

// ─────────────────────────────────────────────────────
//  ADAPTER
// ─────────────────────────────────────────────────────

class RecipeMatchAdapter(
    private val matches: List<RecipeMatch>,
    private val onClick: (RecipeMatch) -> Unit
) : RecyclerView.Adapter<RecipeMatchAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMealType: TextView = view.findViewById(R.id.tvMealType)
        val tvRecipeName: TextView = view.findViewById(R.id.tvRecipeName)
        val tvWhyThis: TextView = view.findViewById(R.id.tvWhyThis)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvCalories: TextView = view.findViewById(R.id.tvCalories)
        val tvScore: TextView = view.findViewById(R.id.tvMatchScore)
        val tvCost: TextView = view.findViewById(R.id.tvEstimatedCost)
        val chipContainer: ViewGroup = view.findViewById(R.id.chipContainer)
        val tvMissingWarning: TextView = view.findViewById(R.id.tvMissingWarning)
        val cardRoot: View = view.findViewById(R.id.cardRecipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_match, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val match = matches[position]
        val recipe = match.recipe

        // Emoji por tipo de plato
        val mealEmoji = when (recipe.mealType) {
            MealType.ENTRADA -> "🥗 Entrada"
            MealType.PLATO_PRINCIPAL -> "🍲 Plato principal"
            MealType.POSTRE -> "🍮 Postre"
            MealType.MERIENDA -> "☕ Merienda"
            MealType.DESAYUNO -> "🍳 Desayuno"
        }

        holder.tvMealType.text = mealEmoji
        holder.tvRecipeName.text = recipe.name
        holder.tvWhyThis.text = match.whyThisRecipe

        val totalTime = recipe.prepTimeMinutes + recipe.cookTimeMinutes
        holder.tvTime.text = "⏱ $totalTime min"
        holder.tvCalories.text = "🔥 ${recipe.caloriesPerServing} kcal/porción"
        holder.tvScore.text = "${(match.matchScore * 100).roundToInt()}% compatible"
        holder.tvCost.text = "💰 ~\$${recipe.estimatedCostUYU} UYU"

        // Chips de ingredientes disponibles
        holder.chipContainer.removeAllViews()
        match.availableIngredients.take(4).forEach { ing ->
            val chip = Chip(holder.itemView.context).apply {
                text = ing
                isClickable = false
                setChipBackgroundColorResource(R.color.chip_available)
            }
            holder.chipContainer.addView(chip)
        }

        // Advertencia de ingredientes faltantes
        if (match.missingIngredients.isNotEmpty()) {
            holder.tvMissingWarning.visibility = View.VISIBLE
            holder.tvMissingWarning.text = "Falta: ${match.missingIngredients.joinToString(", ")}"
        } else {
            holder.tvMissingWarning.visibility = View.GONE
        }

        holder.cardRoot.setOnClickListener { onClick(match) }
    }

    override fun getItemCount() = matches.size
}

// Sesión de receta seleccionada
object RecipeSession {
    var current: RecipeMatch? = null
}
