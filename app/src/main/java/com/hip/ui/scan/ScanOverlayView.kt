package com.hip.ui.scan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.hip.R

/**
 * View personalizada que dibuja el overlay visual sobre la cámara.
 * Indica al usuario dónde apuntar en cada paso del escaneo.
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentStep = 1

    // Paints
    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
        isAntiAlias = true
    }

    private val dimPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(100, 0, 0, 0)
    }

    private val highlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.argb(220, 46, 204, 113)  // Verde HIP
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.WHITE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val arrowPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3f
        color = Color.WHITE
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        when (currentStep) {
            1 -> drawGeneralOverlay(canvas, w, h)
            2 -> drawTopSectorOverlay(canvas, w, h)
            3 -> drawBottomSectorOverlay(canvas, w, h)
        }
    }

    /**
     * Paso 1: Marco general de toda la heladera
     */
    private fun drawGeneralOverlay(canvas: Canvas, w: Float, h: Float) {
        val margin = 40f
        val rect = RectF(margin, margin * 1.5f, w - margin, h - margin * 1.5f)

        // Dimming externo
        drawDimAround(canvas, rect, w, h)

        // Marco principal
        canvas.drawRoundRect(rect, 16f, 16f, framePaint)

        // Esquinas resaltadas
        drawCorners(canvas, rect)

        // Mira central
        val cx = w / 2
        val cy = h / 2
        canvas.drawLine(cx - 20f, cy, cx + 20f, cy, arrowPaint)
        canvas.drawLine(cx, cy - 20f, cx, cy + 20f, arrowPaint)
    }

    /**
     * Paso 2: Sector superior resaltado
     */
    private fun drawTopSectorOverlay(canvas: Canvas, w: Float, h: Float) {
        val margin = 40f
        val midY = h * 0.45f
        val rect = RectF(margin, margin, w - margin, midY)

        // Dim en sector inferior
        val dimBottom = RectF(0f, midY, w, h)
        canvas.drawRect(dimBottom, dimPaint)

        // Marco del sector superior
        canvas.drawRoundRect(rect, 12f, 12f, highlightPaint)
        drawCorners(canvas, rect)

        // Flecha hacia arriba
        drawArrowUp(canvas, w / 2, h * 0.65f)
    }

    /**
     * Paso 3: Sector inferior resaltado
     */
    private fun drawBottomSectorOverlay(canvas: Canvas, w: Float, h: Float) {
        val margin = 40f
        val midY = h * 0.55f
        val rect = RectF(margin, midY, w - margin, h - margin)

        // Dim en sector superior
        val dimTop = RectF(0f, 0f, w, midY)
        canvas.drawRect(dimTop, dimPaint)

        // Marco del sector inferior
        canvas.drawRoundRect(rect, 12f, 12f, highlightPaint)
        drawCorners(canvas, rect)

        // Flecha hacia abajo
        drawArrowDown(canvas, w / 2, h * 0.35f)
    }

    private fun drawDimAround(canvas: Canvas, clearRect: RectF, w: Float, h: Float) {
        val path = Path().apply {
            addRect(0f, 0f, w, h, Path.Direction.CW)
            addRoundRect(clearRect, 16f, 16f, Path.Direction.CCW)
        }
        canvas.drawPath(path, dimPaint)
    }

    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val len = 30f
        val r = 16f

        // Top-left
        canvas.drawLine(rect.left, rect.top + len, rect.left, rect.top + r, cornerPaint)
        canvas.drawLine(rect.left + r, rect.top, rect.left + len, rect.top, cornerPaint)

        // Top-right
        canvas.drawLine(rect.right - len, rect.top, rect.right - r, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top + r, rect.right, rect.top + len, cornerPaint)

        // Bottom-left
        canvas.drawLine(rect.left, rect.bottom - len, rect.left, rect.bottom - r, cornerPaint)
        canvas.drawLine(rect.left + r, rect.bottom, rect.left + len, rect.bottom, cornerPaint)

        // Bottom-right
        canvas.drawLine(rect.right - len, rect.bottom, rect.right - r, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom - r, rect.right, rect.bottom - len, cornerPaint)
    }

    private fun drawArrowUp(canvas: Canvas, x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y - 40f)
            lineTo(x - 20f, y)
            lineTo(x + 20f, y)
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    private fun drawArrowDown(canvas: Canvas, x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y + 40f)
            lineTo(x - 20f, y)
            lineTo(x + 20f, y)
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    fun setCurrentStep(step: Int) {
        currentStep = step
        invalidate()
    }
}
