package com.kropi.hydration.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import com.kropi.hydration.data.Level

/**
 * The in-app equivalent of src/components/hydration/hydration-widget.tsx — same
 * three sizes, same Canvas-drawn ring/mascot/bottle glyphs, reused by both the
 * Home dashboard (large size only) and the Widget tab (all three sizes).
 */
enum class WidgetVariant { SMALL, MEDIUM, LARGE }

@Composable
fun WidgetCard(
    state: HydrationState,
    variant: WidgetVariant,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onPoke: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(KropiColors.card)
            .padding(if (variant == WidgetVariant.SMALL) 12.dp else 18.dp),
    ) {
        when (variant) {
            WidgetVariant.SMALL -> SmallBody(state, onPoke)
            WidgetVariant.MEDIUM -> MediumLargeBody(state, large = false, onAdd, onUndo, onPoke)
            WidgetVariant.LARGE -> MediumLargeBody(state, large = true, onAdd, onUndo, onPoke)
        }
    }
}

@Composable
private fun SmallBody(state: HydrationState, onPoke: () -> Unit) {
    val pct = (state.progress * 100).toInt()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            ProgressRingCanvas(progress = state.progress.toFloat(), modifier = Modifier.fillMaxSize())
            MascotCanvas(level = state.level, modifier = Modifier.size(38.dp).clickable { onPoke() })
        }
        Text("$pct%", color = KropiColors.foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("${state.total} / ${state.goal} ml", color = KropiColors.mutedForeground, fontSize = 10.sp)
    }
}

@Composable
private fun MediumLargeBody(
    state: HydrationState,
    large: Boolean,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onPoke: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(if (large) 88.dp else 68.dp)) {
                ProgressRingCanvas(progress = state.progress.toFloat(), modifier = Modifier.fillMaxSize())
                Text(
                    "${(state.progress * 100).toInt()}%",
                    color = KropiColors.foreground,
                    fontSize = if (large) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Nawodnienie • ${state.streak} dni",
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
                Text(
                    "${state.total} / ${state.goal} ml",
                    color = KropiColors.foreground,
                    fontSize = if (large) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (state.remaining > 0) "Zostało ${state.remaining} ml" else "Cel osiągnięty 🎉",
                    color = KropiColors.aqua,
                    fontSize = 11.sp,
                )
                if (large) {
                    Text(
                        state.selfCare,
                        color = KropiColors.foreground,
                        fontSize = 12.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (large) {
                MascotCanvas(level = state.level, modifier = Modifier.size(72.dp).clickable { onPoke() })
            }
        }

        Spacer(Modifier.height(if (large) 14.dp else 8.dp))
        BottleRow(compact = !large, onAdd = onAdd, onUndo = onUndo)

        if (large) {
            Spacer(Modifier.height(14.dp))
            HourlyBars(state.intakes, state.goal)
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KropiColors.secondary)
                    .padding(10.dp),
            ) {
                Text("Czy wiesz, że… ${state.fact}", color = KropiColors.mutedForeground, fontSize = 11.sp)
            }
        }
    }
}

private val WidgetBottles = listOf(100, 250, 330, 500, 750)

@Composable
fun BottleRow(compact: Boolean, onAdd: (Int) -> Unit, onUndo: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (ml in WidgetBottles) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 56.dp else 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KropiColors.secondary)
                    .clickable { onAdd(ml) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BottleGlyphCanvas(fill = (ml / 750f).coerceIn(0.15f, 1f), modifier = Modifier.width(16.dp).height(28.dp))
                Text("$ml", color = KropiColors.foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(if (compact) 56.dp else 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KropiColors.secondary)
                .clickable { onUndo() },
            contentAlignment = Alignment.Center,
        ) {
            Text("↺", color = KropiColors.mutedForeground, fontSize = 18.sp)
        }
    }
}

@Composable
fun HourlyBars(intakes: List<Intake>, goal: Int) {
    val buckets = (0 until 16).map { i ->
        val h = i + 6
        intakes.filter { it.hour == h }.sumOf { it.ml }
    }
    val max = (goal / 4).coerceAtLeast(buckets.maxOrNull() ?: 1)
    Row(modifier = Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (ml in buckets) {
            val fraction = (ml.toFloat() / max).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((fraction * 36).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (ml > 0) KropiColors.aqua.copy(alpha = 0.85f) else KropiColors.secondary),
                )
            }
        }
    }
}

@Composable
fun ProgressRingCanvas(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.09f
        val d = size.minDimension - stroke
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        drawArc(
            color = KropiColors.secondary,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(d, d),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.linearGradient(listOf(KropiColors.aqua, KropiColors.aquaDeep)),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = Size(d, d),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun MascotCanvas(level: Level, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = kotlin.math.min(size.width, size.height) / 104f
        val cx = size.width / 2f
        val cy = size.height / 2f - 4f * scale

        fun px(x: Float, y: Float) = Offset(cx + x * scale, cy + y * scale)

        val alpha = when (level) {
            Level.LOW -> 0.65f
            Level.MID -> 0.85f
            Level.HIGH, Level.DONE -> 1f
        }

        val body = Path().apply {
            moveTo(px(0f, -48f).x, px(0f, -48f).y)
            cubicTo(
                px(22f, -18f).x, px(22f, -18f).y,
                px(40f, -2f).x, px(40f, -2f).y,
                px(40f, 16f).x, px(40f, 16f).y,
            )
            val arcTopLeft = px(-40f, -24f)
            val arcSize = Size(80f * scale, 80f * scale)
            arcTo(Rect(arcTopLeft, arcSize), 0f, 180f, false)
            cubicTo(
                px(-40f, -2f).x, px(-40f, -2f).y,
                px(-22f, -18f).x, px(-22f, -18f).y,
                px(0f, -48f).x, px(0f, -48f).y,
            )
            close()
        }
        drawPath(
            body,
            brush = Brush.linearGradient(
                listOf(KropiColors.aqua, KropiColors.aquaDeep),
                start = px(0f, -48f),
                end = px(0f, 56f),
            ),
            alpha = alpha,
        )

        val eyeWhite = KropiColors.card.copy(alpha = 0.9f)
        drawOvalAt(px(-22f, -16f), px(-6f, 4f), eyeWhite)
        drawOvalAt(px(6f, -16f), px(22f, 4f), eyeWhite)

        val pupil = Color(0xFF0A1F2C)
        drawCircle(pupil, radius = 4f * scale, center = px(-12f, -4f))
        drawCircle(pupil, radius = 4f * scale, center = px(16f, -4f))

        val mouth = Path()
        val start = when (level) {
            Level.LOW -> Offset(-7f, 9f + 14f)
            Level.MID -> Offset(-7f, 7f + 14f)
            Level.HIGH -> Offset(-8f, 6f + 14f)
            Level.DONE -> Offset(-9f, 5f + 14f)
        }
        val ctrl = when (level) {
            Level.LOW -> Offset(7f, -5f) to Offset(14f, 0f)
            Level.MID -> Offset(7f, 6f) to Offset(14f, 0f)
            Level.HIGH -> Offset(8f, 9f) to Offset(16f, 0f)
            Level.DONE -> Offset(9f, 12f) to Offset(18f, 0f)
        }
        val startPx = px(start.x, start.y)
        mouth.moveTo(startPx.x, startPx.y)
        val controlPx = px(start.x + ctrl.first.x, start.y + ctrl.first.y)
        val endPx = px(start.x + ctrl.second.x, start.y + ctrl.second.y)
        mouth.quadraticBezierTo(controlPx.x, controlPx.y, endPx.x, endPx.y)
        drawPath(mouth, color = KropiColors.card, style = Stroke(width = 4f * scale, cap = StrokeCap.Round))

        val cheek = KropiColors.card.copy(alpha = 0.25f)
        drawOvalAt(px(-31f, 10f), px(-17f, 18f), cheek)
        drawOvalAt(px(19f, 10f), px(33f, 18f), cheek)
    }
}

private fun DrawScope.drawOvalAt(topLeft: Offset, bottomRight: Offset, color: Color) {
    drawOval(color = color, topLeft = topLeft, size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y))
}

@Composable
fun BottleGlyphCanvas(fill: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cap = Size(size.width * 0.3f, size.height * 0.08f)
        drawRoundRect(
            color = KropiColors.aqua.copy(alpha = 0.55f),
            topLeft = Offset((size.width - cap.width) / 2f, 0f),
            size = cap,
            cornerRadius = CornerRadius(2f, 2f),
        )
        val bodyTop = cap.height + size.height * 0.06f
        val bodySize = Size(size.width, size.height - bodyTop)
        drawRoundRect(
            color = KropiColors.aqua.copy(alpha = 0.35f),
            topLeft = Offset(0f, bodyTop),
            size = bodySize,
            cornerRadius = CornerRadius(size.width * 0.35f, size.width * 0.35f),
        )
        val fillHeight = bodySize.height * fill
        drawRoundRect(
            color = KropiColors.aqua,
            topLeft = Offset(size.width * 0.08f, bodyTop + bodySize.height - fillHeight),
            size = Size(size.width * 0.84f, fillHeight),
            cornerRadius = CornerRadius(size.width * 0.3f, size.width * 0.3f),
        )
    }
}

@Composable
fun SizeLabel(text: String) {
    Text(text, color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) KropiColors.aqua else KropiColors.secondary)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) KropiColors.background else KropiColors.foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = KropiColors.mutedForeground, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepperButton("−") { onChange(hour - 1) }
            Text(
                String.format("%02d:00", hour),
                color = KropiColors.foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(56.dp),
            )
            StepperButton("+") { onChange(hour + 1) }
        }
    }
}

@Composable
fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KropiColors.secondary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = KropiColors.foreground, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(KropiColors.card)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Port of src/components/hydration/insight-card.tsx. */
@Composable
fun InsightCard(eyebrow: String, body: String, modifier: Modifier = Modifier, title: String? = null) {
    GlassCard(modifier = modifier) {
        Text(
            eyebrow.uppercase(),
            color = KropiColors.aqua,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
        if (title != null) {
            Text(title, color = KropiColors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(body, color = KropiColors.mutedForeground, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
