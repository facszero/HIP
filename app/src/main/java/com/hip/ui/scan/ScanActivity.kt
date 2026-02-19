package com.hip.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hip.HIPApplication
import com.hip.databinding.ActivityScanBinding
import com.hip.ui.review.IngredientReviewActivity
import com.hip.vision.IngredientDetector
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Actividad de Escaneo Guiado Inteligente.
 *
 * Flujo:
 * PASO 1 → Foto general de la heladera
 * PASO 2 → Sector superior
 * PASO 3 → Sector inferior
 * PASO 4 (opcional) → Cajones/frutera
 *
 * Tras cada foto: análisis ML Kit en background.
 * Al finalizar: muestra todos los ingredientes detectados para validación.
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private val detector = IngredientDetector()

    private var imageCapture: ImageCapture? = null
    private var currentStep = 1
    private val maxSteps = 3

    private val capturedBitmaps = mutableMapOf<Int, Bitmap>()
    private val allDetectedResults = mutableListOf<com.hip.vision.DetectedIngredientResult>()

    // Solicitud de permiso de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else {
            Toast.makeText(this, "Necesitamos permiso de cámara para escanear la heladera", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        updateStepUI()

        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }

        binding.btnSkipStep.setOnClickListener {
            if (currentStep < maxSteps) {
                currentStep++
                updateStepUI()
            } else {
                navigateToReview()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
            } else {
                finish()
            }
        }

        binding.btnAddManual.setOnClickListener {
            // Navegar a revisión directamente con ingredientes ya detectados
            navigateToReview()
        }
    }

    private fun updateStepUI() {
        val (title, subtitle, overlayHint) = when (currentStep) {
            1 -> Triple(
                "Foto general",
                "Abrí la heladera y tomá una foto de todo el interior",
                "Apuntá al centro, capturando toda la heladera"
            )
            2 -> Triple(
                "Sector superior",
                "Enfocá los estantes de arriba",
                "Acercate al sector superior"
            )
            3 -> Triple(
                "Sector inferior",
                "Enfocá los cajones y el sector bajo",
                "Apuntá a la parte inferior y cajones"
            )
            else -> Triple("", "", "")
        }

        binding.tvStepTitle.text = title
        binding.tvStepSubtitle.text = subtitle
        binding.tvOverlayHint.text = overlayHint
        binding.tvStepCounter.text = "Paso $currentStep de $maxSteps"
        binding.progressStep.progress = ((currentStep.toFloat() / maxSteps) * 100).toInt()

        // Actualizar overlay visual
        binding.scanOverlay.setCurrentStep(currentStep)

        // Botón skip para pasos opcionales
        binding.btnSkipStep.text = if (currentStep == maxSteps) "Finalizar" else "Saltear"
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando cámara", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        binding.btnCapture.isEnabled = false
        binding.progressAnalysis.visibility = View.VISIBLE
        binding.tvAnalysisStatus.text = "Analizando imagen..."

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    analyzeCapture(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error capturando foto", exception)
                    binding.btnCapture.isEnabled = true
                    binding.progressAnalysis.visibility = View.GONE
                    Toast.makeText(
                        this@ScanActivity,
                        "Error al capturar. Intentá de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun analyzeCapture(bitmap: Bitmap) {
        capturedBitmaps[currentStep] = bitmap

        lifecycleScope.launch {
            try {
                binding.tvAnalysisStatus.text = "Detectando ingredientes..."
                val results = detector.detectIngredients(bitmap)

                // Agregar resultados únicos al pool global
                results.forEach { newResult ->
                    val exists = allDetectedResults.any {
                        it.normalizedName == newResult.normalizedName
                    }
                    if (!exists) {
                        allDetectedResults.add(newResult)
                    } else {
                        // Actualizar si mayor confianza
                        val idx = allDetectedResults.indexOfFirst { it.normalizedName == newResult.normalizedName }
                        if (idx >= 0 && newResult.confidence > allDetectedResults[idx].confidence) {
                            allDetectedResults[idx] = newResult
                        }
                    }
                }

                binding.tvAnalysisStatus.text = "✓ ${results.size} ingredientes detectados"

                // Avanzar al siguiente paso o finalizar
                if (currentStep < maxSteps) {
                    currentStep++
                    updateStepUI()
                } else {
                    navigateToReview()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error analizando imagen", e)
                binding.tvAnalysisStatus.text = "Error al analizar. Avanzando..."
                if (currentStep < maxSteps) {
                    currentStep++
                    updateStepUI()
                } else {
                    navigateToReview()
                }
            } finally {
                binding.btnCapture.isEnabled = true
                binding.progressAnalysis.visibility = View.GONE
            }
        }
    }

    private fun navigateToReview() {
        val app = application as HIPApplication

        // Guardar resultados en el singleton de sesión
        ScanSession.current.apply {
            detectedResults.clear()
            detectedResults.addAll(allDetectedResults)
        }

        val intent = Intent(this, IngredientReviewActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        detector.release()
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}

// Singleton de sesión de escaneo (en memoria)
object ScanSession {
    val current = Session()

    class Session {
        val detectedResults = mutableListOf<com.hip.vision.DetectedIngredientResult>()
        val confirmedIngredients = mutableListOf<com.hip.model.DetectedIngredient>()
    }
}
