package com.kropi.hydration.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.kropi.hydration.data.Intake
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

/**
 * Wykres "ile i o której" — narastające nawodnienie w czasie.
 *
 * Zastępuje wcześniejsze anonimowe słupki, po których nie dało się odczytać
 * ani godziny, ani objętości. Rysuje: schodkową linię wypitej wody (skok =
 * jeden łyk, podpisany objętością), przerywaną linię planu (od początku do
 * końca okna picia, prosto do celu), znacznik "teraz" oraz oś godzin.
 *
 * Rysuje po gołym Canvasie (nie po bitmapie),
 * żeby ten sam kod obsłużył bitmapę dla Glance i Canvas w aplikacji.
 */
object IntakeChart {

    private const val AQUA = 0xFF00DFE8.toInt()
    private const val AQUA_SOFT = 0x3300DFE8
    private const val TRACK = 0xFF132938.toInt()
    private const val MUTED = 0xFF91A9B3.toInt()
    private const val FOREGROUND = 0xFFEEF7FA.toInt()

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        intakes: List<Intake>,
        goalMl: Int,
        startHour: Int,
        endHour: Int,
        nowMinutes: Int,
    ) {
        if (width <= 0f || height <= 0f) return

        val labelSize = 8.5f * density
        val valueSize = 8f * density
        val left = 1f * density
        val right = width - 1f * density
        val top = 11f * density
        val bottom = height - 12f * density
        if (right - left < 8f * density || bottom - top < 8f * density) return

        val sorted = intakes.sortedBy { it.hour * 60 + it.minute }
        val firstMinute = sorted.firstOrNull()?.let { it.hour * 60 + it.minute }
        val lastMinute = sorted.lastOrNull()?.let { it.hour * 60 + it.minute }
        val startMinute = minOf(startHour * 60, firstMinute ?: (startHour * 60), nowMinutes)
        val endMinute = maxOf(endHour * 60, lastMinute ?: 0, nowMinutes, startMinute + 60)
        val total = sorted.sumOf { it.ml }
        val scaleMax = maxOf(goalMl, total, 1)

        fun x(minute: Int): Float =
            left + (minute - startMinute).toFloat() / (endMinute - startMinute) * (right - left)

        fun y(ml: Int): Float = bottom - (ml.toFloat() / scaleMax) * (bottom - top)

        /** Trzyma etykietę w obrysie wykresu; na bardzo wąskim kafelku centruje. */
        fun clampLabel(centerX: Float, halfWidth: Float): Float {
            val lo = left + halfWidth
            val hi = right - halfWidth
            return if (hi <= lo) (left + right) / 2f else centerX.coerceIn(lo, hi)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        // --- oś i linia celu ---
        linePaint.apply { color = TRACK; strokeWidth = 1f * density; pathEffect = null }
        canvas.drawLine(left, bottom, right, bottom, linePaint)
        linePaint.apply { color = MUTED; alpha = 60 }
        canvas.drawLine(left, y(goalMl), right, y(goalMl), linePaint)
        linePaint.alpha = 255

        // --- linia planu: od zera na starcie okna do celu na jego końcu ---
        linePaint.apply {
            color = MUTED
            alpha = 150
            strokeWidth = 1.2f * density
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f * density, 3f * density), 0f)
        }
        canvas.drawLine(x(startHour * 60), y(0), x(endHour * 60), y(goalMl), linePaint)
        linePaint.pathEffect = null
        linePaint.alpha = 255

        // --- schodki faktycznie wypitej wody ---
        val stepPath = Path()
        val dots = ArrayList<FloatArray>()
        var cumulative = 0
        stepPath.moveTo(x(startMinute), y(0))
        for (intake in sorted) {
            val minute = intake.hour * 60 + intake.minute
            stepPath.lineTo(x(minute), y(cumulative))
            cumulative += intake.ml
            stepPath.lineTo(x(minute), y(cumulative))
            dots.add(floatArrayOf(x(minute), y(cumulative), intake.ml.toFloat()))
        }
        val edgeMinute = maxOf(nowMinutes, lastMinute ?: startMinute)
        stepPath.lineTo(x(edgeMinute), y(cumulative))

        val fillPath = Path(stepPath).apply {
            lineTo(x(edgeMinute), bottom)
            lineTo(x(startMinute), bottom)
            close()
        }
        canvas.drawPath(fillPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AQUA_SOFT })
        linePaint.apply { color = AQUA; strokeWidth = 2f * density; strokeCap = Paint.Cap.ROUND }
        canvas.drawPath(stepPath, linePaint)

        // --- kropka + objętość przy każdym łyku (etykiety kolidujące pomijamy) ---
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AQUA }
        textPaint.apply { color = FOREGROUND; textSize = valueSize }
        var occupiedUntil = Float.NEGATIVE_INFINITY
        for (dot in dots) {
            canvas.drawCircle(dot[0], dot[1], 2.2f * density, dotPaint)
            val label = dot[2].toInt().toString()
            val halfWidth = textPaint.measureText(label) / 2f
            val cx = clampLabel(dot[0], halfWidth)
            if (cx - halfWidth < occupiedUntil) continue
            canvas.drawText(label, cx, (dot[1] - 5f * density).coerceAtLeast(valueSize), textPaint)
            occupiedUntil = cx + halfWidth + 4f * density
        }

        // --- znacznik "teraz" ---
        val nowX = x(nowMinutes.coerceIn(startMinute, endMinute))
        linePaint.apply {
            color = FOREGROUND
            alpha = 110
            strokeWidth = 1f * density
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f * density, 3f * density), 0f)
        }
        canvas.drawLine(nowX, top - 4f * density, nowX, bottom, linePaint)
        linePaint.pathEffect = null
        linePaint.alpha = 255

        // --- oś godzin; etykieta "teraz" wypiera kolidującą godzinę ---
        val baseline = height - 2f * density
        textPaint.apply { color = MUTED; textSize = labelSize }
        val nowLabelHalf = textPaint.measureText("teraz") / 2f
        val spanHours = (endMinute - startMinute) / 60
        val hourStep = ((spanHours + 4) / 5).coerceAtLeast(1)
        var hour = startHour
        while (hour * 60 <= endMinute) {
            val hx = x(hour * 60)
            val label = "$hour"
            val halfWidth = textPaint.measureText(label) / 2f
            val cx = clampLabel(hx, halfWidth)
            val clashesWithNow = kotlin.math.abs(cx - nowX) < nowLabelHalf + halfWidth + 3f * density
            if (!clashesWithNow) canvas.drawText(label, cx, baseline, textPaint)
            hour += hourStep
        }
        textPaint.color = AQUA
        canvas.drawText("teraz", clampLabel(nowX, nowLabelHalf), baseline, textPaint)
    }
}

/**
 * Glance nie ma własnego Canvasu, więc wykres trafia na widget jako bitmapa —
 * tak samo jak pierścień postępu i maskotka.
 */
fun intakeChartBitmap(
    widthPx: Int,
    heightPx: Int,
    density: Float,
    intakes: List<Intake>,
    goalMl: Int,
    startHour: Int,
    endHour: Int,
    nowMinutes: Int,
): Bitmap {
    val width = widthPx.coerceAtLeast(1)
    val height = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    IntakeChart.draw(
        canvas = Canvas(bitmap),
        width = width.toFloat(),
        height = height.toFloat(),
        density = density,
        intakes = intakes,
        goalMl = goalMl,
        startHour = startHour,
        endHour = endHour,
        nowMinutes = nowMinutes,
    )
    return bitmap
}
