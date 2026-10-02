package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

enum class PatternState {
    DEFAULT,
    SUCCESS,
    ERROR
}

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    patternState: PatternState = PatternState.DEFAULT,
    enabled: Boolean = true,
    dotColor: Color = Color(0xFF6B7280),
    activeColor: Color = Color(0xFFF43F5E),
    successColor: Color = Color(0xFF10B981),
    errorColor: Color = Color(0xFFEF4444),
    onPatternComplete: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    // Reset when patternState changes back to default
    LaunchedEffect(patternState) {
        if (patternState == PatternState.DEFAULT) {
            selectedDots.clear()
            currentTouchPosition = null
        }
    }

    val lineColor by animateColorAsState(
        targetValue = when (patternState) {
            PatternState.DEFAULT -> activeColor
            PatternState.SUCCESS -> successColor
            PatternState.ERROR -> errorColor
        },
        animationSpec = tween(durationMillis = 200),
        label = "patternLineColor"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("pattern_lock_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, patternState) {
                    if (!enabled) return@pointerInput

                    awaitEachGesture {
                        val down: PointerInputChange = awaitFirstDown()
                        selectedDots.clear()

                        val width = size.width
                        val height = size.height
                        val cellW = width / 3f
                        val cellH = height / 3f
                        val hitRadius = minOf(cellW, cellH) * 0.42f

                        fun getDotAt(offset: Offset): Int? {
                            for (row in 0 until 3) {
                                for (col in 0 until 3) {
                                    val centerX = col * cellW + cellW / 2f
                                    val centerY = row * cellH + cellH / 2f
                                    val dist = hypot(offset.x - centerX, offset.y - centerY)
                                    if (dist <= hitRadius) {
                                        return row * 3 + col
                                    }
                                }
                            }
                            return null
                        }

                        // Check initial touch
                        val initialDot = getDotAt(down.position)
                        if (initialDot != null) {
                            selectedDots.add(initialDot)
                            triggerVibration()
                        }
                        currentTouchPosition = down.position

                        // Track continuous dragging
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }

                            currentTouchPosition = change.position
                            val dot = getDotAt(change.position)
                            if (dot != null && !selectedDots.contains(dot)) {
                                selectedDots.add(dot)
                                triggerVibration()
                            }
                            change.consume()
                        }

                        // Touch lifted: trigger completion if user selected dots
                        currentTouchPosition = null
                        if (selectedDots.isNotEmpty()) {
                            onPatternComplete(selectedDots.toList())
                        }
                    }
                }
        ) {
            val cellW = size.width / 3f
            val cellH = size.height / 3f

            fun getCenterOfDot(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
            }

            // 1. Draw connecting lines between selected dots
            if (selectedDots.size > 1) {
                val path = Path().apply {
                    val first = getCenterOfDot(selectedDots.first())
                    moveTo(first.x, first.y)
                    for (i in 1 until selectedDots.size) {
                        val pt = getCenterOfDot(selectedDots[i])
                        lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 2. Draw live line from last selected dot to current finger touch
            if (selectedDots.isNotEmpty() && currentTouchPosition != null && patternState == PatternState.DEFAULT) {
                val lastCenter = getCenterOfDot(selectedDots.last())
                drawLine(
                    color = lineColor.copy(alpha = 0.85f),
                    start = lastCenter,
                    end = currentTouchPosition!!,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw 9 pattern nodes (outer ring + inner dot + active glow)
            for (index in 0 until 9) {
                val center = getCenterOfDot(index)
                val isSelected = selectedDots.contains(index)
                val nodeColor = if (isSelected) lineColor else dotColor

                if (isSelected) {
                    // Outer glow aura
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.22f),
                        radius = 26.dp.toPx(),
                        center = center
                    )
                    // Outer active ring
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.65f),
                        radius = 16.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Inner filled core
                    drawCircle(
                        color = nodeColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                } else {
                    // Inactive subtle dot
                    drawCircle(
                        color = dotColor.copy(alpha = 0.7f),
                        radius = 7.dp.toPx(),
                        center = center
                    )
                    // Dim surrounding guide ring
                    drawCircle(
                        color = dotColor.copy(alpha = 0.15f),
                        radius = 18.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}
