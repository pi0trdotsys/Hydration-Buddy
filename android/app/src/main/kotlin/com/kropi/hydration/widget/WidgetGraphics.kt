package com.kropi.hydration.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.kropi.hydration.data.Level

/**
 * The Glance widget API has no arbitrary Canvas/SVG rendering, so the
 * progress ring and mascot from the TS mockup (progress-ring.tsx,
 * mascot-drop.tsx — both raw <svg>) are pre-rendered to bitmaps here and
 * shown via Image(ImageProvider(bitmap)). Bottles and bar charts are plain
 * Glance boxes (see HydrationWidgetUi.kt) since those only need rectangles.
 */
object WidgetGraphics {

    private const val AQUA = 0xFF00DFE8.toInt()
    private const val AQUA_DEEP = 0xFF007BB2.toInt()
    private const val TRACK = 0xFF132938.toInt()
    private const val CARD = 0xFF071A27.toInt()

    /** Mirrors ProgressRing.tsx: a gradient arc over a flat track, starting at 12 o'clock. */
    fun progressRing(sizePx: Int, strokePx: Float, progress: Float): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pad = strokePx / 2f + 2f
        val oval = RectF(pad, pad, sizePx - pad, sizePx - pad)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            color = TRACK
        }
        canvas.drawOval(oval, trackPaint)

        val sweep = 360f * progress.coerceIn(0f, 1f)
        if (sweep > 0f) {
            val gradient = LinearGradient(
                oval.left, oval.top, oval.right, oval.bottom,
                AQUA, AQUA_DEEP, Shader.TileMode.CLAMP,
            )
            val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx
                strokeCap = Paint.Cap.ROUND
                shader = gradient
            }
            canvas.drawArc(oval, -90f, sweep, false, fgPaint)
        }
        return bmp
    }

    /** Mirrors mascot-drop.tsx's inline <svg> path 1:1 (viewBox -50 -55 100 110). */
    fun mascot(sizePx: Int, level: Level): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.translate(sizePx / 2f, sizePx / 2f)
        val scale = (sizePx * 0.86f) / 104f
        canvas.scale(scale, scale)
        canvas.translate(0f, -4f) // shape's own vertical center sits at y=4, not 0

        val alpha = when (level) {
            Level.LOW -> 0.65f
            Level.MID -> 0.85f
            Level.HIGH, Level.DONE -> 1f
        }

        val body = Path().apply {
            moveTo(0f, -48f)
            cubicTo(22f, -18f, 40f, -2f, 40f, 16f)
            arcTo(RectF(-40f, -24f, 40f, 56f), 0f, 180f, false)
            cubicTo(-40f, -2f, -22f, -18f, 0f, -48f)
            close()
        }
        val bodyGradient = LinearGradient(0f, -48f, 0f, 56f, AQUA, AQUA_DEEP, Shader.TileMode.CLAMP)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = bodyGradient
            this.alpha = (alpha * 255).toInt()
        }
        canvas.drawPath(body, bodyPaint)

        val eyeWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD; this.alpha = 230 }
        canvas.drawOval(RectF(-22f, -16f, -6f, 4f), eyeWhite)
        canvas.drawOval(RectF(6f, -16f, 22f, 4f), eyeWhite)

        val pupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A1F2C.toInt() }
        canvas.drawCircle(-12f, -4f, 4f, pupil)
        canvas.drawCircle(16f, -4f, 4f, pupil)

        val mouth = Path()
        val (dx1, dy1, dx2, dy2) = when (level) {
            Level.LOW -> Quad(7f, -5f, 14f, 0f)
            Level.MID -> Quad(7f, 6f, 14f, 0f)
            Level.HIGH -> Quad(8f, 9f, 16f, 0f)
            Level.DONE -> Quad(9f, 12f, 18f, 0f)
        }
        val startX = when (level) {
            Level.LOW -> -7f
            Level.MID -> -7f
            Level.HIGH -> -8f
            Level.DONE -> -9f
        }
        val startY = when (level) {
            Level.LOW -> 9f
            Level.MID -> 7f
            Level.HIGH -> 6f
            Level.DONE -> 5f
        } + 14f
        mouth.moveTo(startX, startY)
        mouth.rQuadTo(dx1, dy1, dx2, dy2)
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            color = CARD
        }
        canvas.drawPath(mouth, mouthPaint)

        val cheek = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD; this.alpha = 64 }
        canvas.drawOval(RectF(-31f, 10f, -17f, 18f), cheek)
        canvas.drawOval(RectF(19f, 10f, 33f, 18f), cheek)

        return bmp
    }

    private data class Quad(val dx1: Float, val dy1: Float, val dx2: Float, val dy2: Float)
}
