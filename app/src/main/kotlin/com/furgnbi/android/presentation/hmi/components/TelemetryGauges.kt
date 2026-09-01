package com.furgnbi.android.presentation.hmi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furgnbi.android.presentation.theme.*

@Composable
fun TelemetryGaugesRow(
    temperature1: Float?,
    temperature2: Float?,
    humidity: Float?,
    voltage: Float?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // T1: Zona 1 Congelados
        TelemetryCard(
            title = "T1 — ZONA CONGELADOS",
            value = temperature1?.let { String.format("%.1f", it) } ?: "--.-",
            unit = "°C",
            icon = Icons.Default.Thermostat,
            accentColor = FrostBlue,
            statusLabel = if (temperature1 != null && temperature1 > -10.0f) "ALTA" else "NORMAL",
            statusColor = if (temperature1 != null && temperature1 > -10.0f) WarningAmber else SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        // T2: Zona 2 Refrigerados
        TelemetryCard(
            title = "T2 — ZONA REFRIGERADOS",
            value = temperature2?.let { String.format("%.1f", it) } ?: "--.-",
            unit = "°C",
            icon = Icons.Default.Thermostat,
            accentColor = ColdCyan,
            statusLabel = if (temperature2 != null && (temperature2 < 0f || temperature2 > 10f)) "ALERTA" else "NORMAL",
            statusColor = if (temperature2 != null && (temperature2 < 0f || temperature2 > 10f)) WarningAmber else SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        // Humedad
        TelemetryCard(
            title = "HUMEDAD RELATIVA",
            value = humidity?.let { String.format("%.1f", it) } ?: "--.-",
            unit = "%HR",
            icon = Icons.Default.WaterDrop,
            accentColor = AccentCyan,
            statusLabel = if (humidity != null && humidity > 90f) "SATURADA" else "ÓPTIMA",
            statusColor = if (humidity != null && humidity > 90f) WarningAmber else SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        // Voltaje
        TelemetryCard(
            title = "VOLTAJE SISTEMA",
            value = voltage?.let { String.format("%.1f", it) } ?: "--.-",
            unit = "V",
            icon = Icons.Default.Bolt,
            accentColor = PrimaryBlue,
            statusLabel = if (voltage != null && voltage < 22f) "BAJO" else "CORRECTO",
            statusColor = if (voltage != null && voltage < 22f) DangerRed else SuccessGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Title & Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value + Unit
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, RoundedCornerShape(50))
                )
                Text(
                    text = statusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
