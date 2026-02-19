package com.hip.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hip.HIPApplication
import com.hip.databinding.ActivityMainBinding
import com.hip.ui.profile.ProfileSetupActivity
import com.hip.ui.scan.ScanActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Pantalla principal de HIP.
 *
 * Opciones:
 * 🔍 Escanear heladera → ScanActivity
 * 👤 Mi perfil nutricional → ProfileSetupActivity
 * ⚙ Configuración (LLM, privacidad)
 *
 * Si es primera vez → redirigir a configuración de perfil primero.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as HIPApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkFirstLaunch()
        setupUI()
        loadUserInfo()
    }

    private fun checkFirstLaunch() {
        lifecycleScope.launch {
            val isFirst = app.preferencesManager.isFirstLaunchFlow.first()
            if (isFirst) {
                startActivity(Intent(this@MainActivity, ProfileSetupActivity::class.java))
                finish()
            }
        }
    }

    private fun setupUI() {
        // Botón principal: escanear
        binding.btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // Perfil nutricional
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileSetupActivity::class.java))
        }

        // Card de objetivo nutricional
        binding.cardGoal.setOnClickListener {
            startActivity(Intent(this, ProfileSetupActivity::class.java))
        }

        // Botón de privacidad/configuración
        binding.btnSettings.setOnClickListener {
            showPrivacyInfo()
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val profile = app.preferencesManager.getUserProfile()

            // Mostrar objetivo actual
            binding.tvCurrentGoal.text = when (profile.goal) {
                com.hip.model.NutritionalGoal.BAJAR_PESO -> "🎯 Bajar peso"
                com.hip.model.NutritionalGoal.MANTENER -> "⚖ Mantener peso"
                com.hip.model.NutritionalGoal.AUMENTAR_MASA -> "💪 Aumentar masa"
                com.hip.model.NutritionalGoal.CONTROL_GLUCEMICO -> "🩺 Control glucémico"
                com.hip.model.NutritionalGoal.BAJO_SODIO -> "🧂 Bajo en sodio"
                com.hip.model.NutritionalGoal.ALTO_PROTEINA -> "🥩 Alta proteína"
                com.hip.model.NutritionalGoal.NINGUNO -> "Sin objetivo definido"
            }

            // Nivel de cocina
            binding.tvCookingLevel.text = when (profile.cookingLevel) {
                com.hip.model.CookingLevel.PRINCIPIANTE -> "Principiante"
                com.hip.model.CookingLevel.INTERMEDIO -> "Intermedio"
                com.hip.model.CookingLevel.AVANZADO -> "Avanzado"
            }

            // Alergias
            binding.tvAllergies.text = if (profile.allergies.isEmpty()) {
                "Sin alergias registradas"
            } else {
                profile.allergies.joinToString(", ")
            }

            // Estado LLM
            val llmEnabled = app.preferencesManager.llmEnabledFlow.first()
            binding.tvLLMStatus.text = if (llmEnabled) "✓ Adaptación IA activada" else "Modo local (sin IA)"
        }
    }

    private fun showPrivacyInfo() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔒 Privacidad")
            .setMessage(
                "HIP respeta tu privacidad:\n\n" +
                        "• Las fotos de tu heladera se procesan localmente y NUNCA se almacenan.\n\n" +
                        "• Si activás la adaptación IA, solo se envía texto (nombres de ingredientes) al servicio cloud. Nunca imágenes.\n\n" +
                        "• No se vinculan datos a tu perfil comercial.\n\n" +
                        "• No se comparte información con terceros.\n\n" +
                        "• Podés usar modo offline sin conectividad."
            )
            .setPositiveButton("Entendido", null)
            .setNeutralButton("Configurar IA") { _, _ ->
                showLLMConfigDialog()
            }
            .show()
    }

    private fun showLLMConfigDialog() {
        val view = layoutInflater.inflate(com.hip.R.layout.dialog_llm_config, null)
        val etApiKey = view.findViewById<android.widget.EditText>(com.hip.R.id.etApiKey)
        val switchLLM = view.findViewById<android.widget.Switch>(com.hip.R.id.switchLLMEnabled)

        lifecycleScope.launch {
            val currentKey = app.preferencesManager.llmApiKeyFlow.first()
            val enabled = app.preferencesManager.llmEnabledFlow.first()
            etApiKey.setText(if (currentKey.isNotBlank()) "****" else "")
            switchLLM.isChecked = enabled
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configuración IA")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                lifecycleScope.launch {
                    val key = etApiKey.text.toString().trim()
                    if (key.isNotBlank() && key != "****") {
                        app.preferencesManager.saveLLMApiKey(key)
                    }
                    app.preferencesManager.setLLMEnabled(switchLLM.isChecked)
                    loadUserInfo()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
