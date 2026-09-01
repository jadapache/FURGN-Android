package com.furgnbi.android.presentation.hmi.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furgnbi.android.presentation.theme.*

@Composable
fun TruckCanvas(
    fan1: Boolean,
    fan2: Boolean,
    fan3: Boolean,
    fan4: Boolean,
    fan5: Boolean,
    fan6: Boolean,
    gateOpen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Truck Outline Drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background grid pattern
            val trailerLeft = w * 0.16f
            val trailerTop = h * 0.12f
            val trailerWidth = w * 0.80f
            val trailerHeight = h * 0.65f

            // 1. Draw Trailer insulated box (Outer glow / border)
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(trailerLeft, trailerTop),
                size = Size(trailerWidth, trailerHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = PrimaryBlueVariant.copy(alpha = 0.5f),
                topLeft = Offset(trailerLeft, trailerTop),
                size = Size(trailerWidth, trailerHeight),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 3f)
            )

            // 2. Zone separator bulkhead (Dividing line inside trailer)
            val zoneDividerX = trailerLeft + (trailerWidth * 0.48f)
            drawLine(
                color = AccentCyan.copy(alpha = 0.6f),
                start = Offset(zoneDividerX, trailerTop + 10f),
                end = Offset(zoneDividerX, trailerTop + trailerHeight - 10f),
                strokeWidth = 3f
            )

            // 3. Cabin (Front truck cab)
            val cabRight = trailerLeft - 4f
            val cabLeft = w * 0.03f
            val cabTop = h * 0.32f
            val cabBottom = trailerTop + trailerHeight

            val cabPath = Path().apply {
                moveTo(cabRight, cabTop)
                lineTo(cabLeft + 20f, cabTop + 10f)
                lineTo(cabLeft, cabTop + 45f)
                lineTo(cabLeft, cabBottom)
                lineTo(cabRight, cabBottom)
                close()
            }
            drawPath(path = cabPath, color = Color(0xFF334155))
            drawPath(path = cabPath, color = Color(0xFF475569), style = Stroke(2f))

            // Cabin Window
            val winPath = Path().apply {
                moveTo(cabRight - 8f, cabTop + 8f)
                lineTo(cabLeft + 24f, cabTop + 16f)
                lineTo(cabLeft + 12f, cabTop + 45f)
                lineTo(cabRight - 8f, cabTop + 45f)
                close()
            }
            drawPath(path = winPath, color = PrimaryBlue.copy(alpha = 0.35f))

            // 4. Wheels
            val wheelRadius = h * 0.08f
            val wheelY = trailerTop + trailerHeight + 4f
            val wheelPositions = listOf(
                cabLeft + (cabRight - cabLeft) * 0.5f, // Cab wheel
                trailerLeft + trailerWidth * 0.68f,   // Rear wheel 1
                trailerLeft + trailerWidth * 0.86f    // Rear wheel 2
            )
            wheelPositions.forEach { wx ->
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = wheelRadius,
                    center = Offset(wx, wheelY)
                )
                drawCircle(
                    color = Color(0xFF64748B),
                    radius = wheelRadius,
                    center = Offset(wx, wheelY),
                    style = Stroke(3f)
                )
                drawCircle(
                    color = Color(0xFF94A3B8),
                    radius = wheelRadius * 0.4f,
                    center = Offset(wx, wheelY)
                )
            }
        }

        // Zone 1 & Zone 2 Labels and Indicators
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 120.dp, end = 20.dp, top = 20.dp, bottom = 45.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Zona 1 (Congelados)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ZONA 1 — CONGELADOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FrostBlue
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FanIndicator(name = "V1", isOn = fan1)
                    FanIndicator(name = "V2", isOn = fan2)
                    FanIndicator(name = "V3", isOn = fan3)
                }

                Text(
                    text = "Evaporador Primario",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Zona 2 (Refrigerados) + Gate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ZONA 2 — REFRIGERADOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColdCyan
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FanIndicator(name = "V4", isOn = fan4)
                    FanIndicator(name = "V5", isOn = fan5)
                    FanIndicator(name = "V6", isOn = fan6)
                    GateIndicator(isOpen = gateOpen)
                }

                Text(
                    text = "Evaporador Secundario & Compuerta",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun FanIndicator(
    name: String,
    isOn: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fan_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isOn) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fan_spin"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isOn) FanActive.copy(alpha = 0.15f) else FanInactive.copy(alpha = 0.15f),
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isOn) FanActive else FanInactive,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = name,
                tint = if (isOn) SuccessGreenGlow else TextMuted,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (isOn) angle else 0f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isOn) TextPrimary else TextSecondary
        )
        Text(
            text = if (isOn) "ON" else "OFF",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOn) SuccessGreen else TextMuted
        )
    }
}

@Composable
fun GateIndicator(
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gate_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gate_alert"
    )

    val currentAlpha = if (isOpen) alpha else 1f
    val color = if (isOpen) DangerRed.copy(alpha = currentAlpha) else GateClosed

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .border(2.dp, color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOpen) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = "Compuerta",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "PUERTA",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Text(
            text = if (isOpen) "ABIERTA" else "CERRADA",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
