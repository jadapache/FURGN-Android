package com.furgnbi.android.presentation.hmi.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furgnbi.android.domain.model.ModbusConnectionState
import com.furgnbi.android.domain.model.MqttConnectionState
import com.furgnbi.android.presentation.theme.*

@Composable
fun StatusBar(
    deviceId: String,
    modbusState: ModbusConnectionState,
    mqttState: MqttConnectionState,
    isGpsAvailable: Boolean,
    latitude: Double?,
    longitude: Double?,
    speedKmh: Float?,
    alarmCount: Int,
    onAlarmsClick: () -> Unit = {},
    onConfigClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Device ID / Unit identifier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Unidad",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = deviceId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Modbus Status Pill
                StatusPill(
                    label = "MODBUS",
                    statusText = when (modbusState) {
                        ModbusConnectionState.CONNECTED -> "OK"
                        ModbusConnectionState.CONNECTING -> "..."
                        ModbusConnectionState.DISCONNECTED -> "OFF"
                        ModbusConnectionState.ERROR -> "ERR"
                    },
                    color = when (modbusState) {
                        ModbusConnectionState.CONNECTED -> SuccessGreen
                        ModbusConnectionState.CONNECTING -> WarningAmber
                        ModbusConnectionState.DISCONNECTED -> TextMuted
                        ModbusConnectionState.ERROR -> DangerRed
                    }
                )

                // MQTT Status Pill
                StatusPill(
                    label = "MQTT",
                    statusText = when (mqttState) {
                        MqttConnectionState.CONNECTED -> "ONLINE"
                        MqttConnectionState.CONNECTING -> "CON..."
                        MqttConnectionState.RECONNECTING -> "REINT."
                        MqttConnectionState.DISCONNECTED -> "OFFLINE"
                        MqttConnectionState.ERROR -> "ERROR"
                    },
                    color = when (mqttState) {
                        MqttConnectionState.CONNECTED -> SuccessGreen
                        MqttConnectionState.CONNECTING,
                        MqttConnectionState.RECONNECTING -> WarningAmber
                        MqttConnectionState.DISCONNECTED -> TextMuted
                        MqttConnectionState.ERROR -> DangerRed
                    }
                )
            }

            // Right: GPS, Alarms, and Config Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // GPS status & Coordinates
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = if (isGpsAvailable) SuccessGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    if (latitude != null && longitude != null) {
                        Text(
                            text = String.format("%.4f, %.4f", latitude, longitude),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (speedKmh != null) {
                            Text(
                                text = "• ${speedKmh.toInt()} km/h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    } else {
                        Text(
                            text = if (isGpsAvailable) "Buscando..." else "Sin GPS",
                            fontSize = 11.sp,
                            color = if (isGpsAvailable) WarningAmber else TextMuted
                        )
                    }
                }

                // Alarm Badge
                AlarmBadge(
                    count = alarmCount,
                    onClick = onAlarmsClick
                )

                // Config button
                IconButton(
                    onClick = onConfigClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    statusText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label: $statusText",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun AlarmBadge(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val hasAlarms = count > 0
    val badgeColor = if (hasAlarms) DangerRed.copy(alpha = alpha) else SurfaceVariantDark

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hasAlarms) DangerRed.copy(alpha = 0.2f) else SurfaceVariantDark)
            .border(1.dp, if (hasAlarms) DangerRed else BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Alarmas",
                tint = if (hasAlarms) DangerRedGlow else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasAlarms) DangerRedGlow else TextSecondary
            )
        }
    }
}
