package com.hip.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.hip.HIPApplication
import com.hip.R
import com.hip.databinding.ActivityProfileSetupBinding
import com.hip.model.CookingLevel
import com.hip.model.NutritionalGoal
import com.hip.model.UserProfile
import com.hip.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Pantalla de configuración del perfil nutricional y de cocina.
 *
 * - Objetivo nutricional
 * - Nivel de cocina
 * - Alergias comunes
 * - Equipamiento disponible
 * - Tiempo máximo de cocina
 */
class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val app by lazy { application as HIPApplication }

    private var selectedGoal = NutritionalGoal.MANTENER
    private var selectedLevel = CookingLevel.INTERMEDIO
    private val selectedAllergies = mutableSetOf<String>()
    private var maxCookingTime = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            // Cargar perfil existente si hay
            val profile = app.preferencesManager.getUserProfile()
            loadExistingProfile(profile)
        }

        setupGoalChips()
        setupLevelChips()
        setupAllergyChips()
        setupEquipmentSection()
        setupTimeSlider()
        setupSaveButton()
    }

    private fun loadExistingProfile(profile: UserProfile) {
        selectedGoal = profile.goal
        selectedLevel = profile.cookingLevel
        selectedAllergies.addAll(profile.allergies)
        maxCookingTime = profile.maxCookingTimeMinutes
        binding.checkHasOven.isChecked = profile.hasOven
        binding.checkHasMicrowave.isChecked = profile.hasMicrowave
        binding.checkHasBlender.isChecked = profile.hasBlender
        updateTimeLabel()
    }

    private fun setupGoalChips() {
        val goals = mapOf(
            NutritionalGoal.BAJAR_PESO to "⬇ Bajar peso",
            NutritionalGoal.MANTENER to "⚖ Mantener",
            NutritionalGoal.AUMENTAR_MASA to "💪 Aumentar masa",
            NutritionalGoal.CONTROL_GLUCEMICO to "🩺 Control glucémico",
            NutritionalGoal.BAJO_SODIO to "🧂 Bajo sodio",
            NutritionalGoal.ALTO_PROTEINA to "🥩 Alta proteína"
        )

        goals.forEach { (goal, label) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = goal == selectedGoal
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedGoal = goal
                }
            }
            binding.chipGroupGoal.addView(chip)
        }
    }

    private fun setupLevelChips() {
        val levels = mapOf(
            CookingLevel.PRINCIPIANTE to "🌱 Principiante",
            CookingLevel.INTERMEDIO to "👨‍🍳 Intermedio",
            CookingLevel.AVANZADO to "⭐ Avanzado"
        )

        levels.forEach { (level, label) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = level == selectedLevel
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedLevel = level
                }
            }
            binding.chipGroupLevel.addView(chip)
        }
    }

    private fun setupAllergyChips() {
        val commonAllergies = listOf(
            "Gluten", "Lactosa", "Huevo", "Mariscos",
            "Nueces", "Maní", "Soja", "Maíz"
        )

        commonAllergies.forEach { allergy ->
            val chip = Chip(this).apply {
                text = allergy
                isCheckable = true
                isChecked = allergy in selectedAllergies
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedAllergies.add(allergy)
                    else selectedAllergies.remove(allergy)
                }
            }
            binding.chipGroupAllergies.addView(chip)
        }
    }

    private fun setupEquipmentSection() {
        // Checkboxes ya en el layout
    }

    private fun setupTimeSlider() {
        binding.seekBarTime.max = 120  // máx 2 horas
        binding.seekBarTime.progress = maxCookingTime
        updateTimeLabel()

        binding.seekBarTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxCookingTime = progress.coerceAtLeast(10)
                updateTimeLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateTimeLabel() {
        binding.tvTimeLabel.text = when {
            maxCookingTime <= 20 -> "Máx $maxCookingTime min (recetas muy rápidas)"
            maxCookingTime <= 45 -> "Máx $maxCookingTime min"
            maxCookingTime <= 60 -> "Máx $maxCookingTime min (1 hora)"
            else -> "Máx $maxCookingTime min (hasta ${maxCookingTime/60}h ${maxCookingTime%60}min)"
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnSkipProfile.setOnClickListener {
            goToMain()
        }
    }

    private fun saveProfile() {
        val profile = UserProfile(
            goal = selectedGoal,
            cookingLevel = selectedLevel,
            allergies = selectedAllergies.toList(),
            hasOven = binding.checkHasOven.isChecked,
            hasMicrowave = binding.checkHasMicrowave.isChecked,
            hasBlender = binding.checkHasBlender.isChecked,
            maxCookingTimeMinutes = maxCookingTime
        )

        lifecycleScope.launch {
            app.preferencesManager.saveUserProfile(profile)
            app.preferencesManager.setFirstLaunchCompleted()
            goToMain()
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
