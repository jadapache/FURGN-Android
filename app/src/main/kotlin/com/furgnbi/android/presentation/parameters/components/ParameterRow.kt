package com.furgnbi.android.presentation.parameters.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furgnbi.android.presentation.parameters.ParameterUiState
import com.furgnbi.android.presentation.theme.*

@Composable
fun ParameterRow(
    item: ParameterUiState,
    onValueChange: (String) -> Unit,
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val param = item.parameter
    val isReadOnly = param.isReadOnly
    val hasError = item.validationError != null
    val isDirty = item.isDirty

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isDirty) WarningAmber else if (hasError) DangerRed else BorderSubtle
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: ID + Name + Address Badge + Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${param.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Text(
                        text = param.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modbus Address Pill
                    Text(
                        text = "HR ${param.modbusAddress} • ${param.modbusDataType}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier
                            .background(SurfaceVariantDark, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    if (isReadOnly) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .background(WarningAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Solo Lectura",
                                tint = WarningAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "READ-ONLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    }
                }
            }

            // Description & Range limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = param.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Rango: [${param.minValue} .. ${param.maxValue} ${param.unit}]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan
                )
            }

            Divider(color = BorderSubtle, thickness = 0.8.dp)

            // Value Editor Row: Current Value | Input Field | Write Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current stored value
                Column {
                    Text(
                        text = "VALOR ACTUAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = param.currentValue?.let { "$it ${param.unit}" } ?: "No leído",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (param.currentValue != null) TextPrimary else WarningAmber
                    )
                }

                // Input Field & Action Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedTextField(
                            value = item.inputString,
                            onValueChange = onValueChange,
                            enabled = !isReadOnly && !item.isWriting,
                            singleLine = true,
                            isError = hasError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                if (param.unit.isNotBlank()) {
                                    Text(
                                        text = param.unit,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark,
                                disabledContainerColor = SurfaceVariantDark.copy(alpha = 0.4f),
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderSubtle,
                                errorBorderColor = DangerRed,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextMuted
                            ),
                            modifier = Modifier.width(140.dp)
                        )

                        if (hasError) {
                            Text(
                                text = item.validationError ?: "",
                                fontSize = 10.sp,
                                color = DangerRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Write Button
                    Button(
                        onClick = onWriteClick,
                        enabled = !isReadOnly && !hasError && item.inputString.isNotBlank() && !item.isWriting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDirty) WarningAmber else PrimaryBlue,
                            contentColor = BackgroundDark,
                            disabledContainerColor = SurfaceVariantDark,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (item.isWriting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BackgroundDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Escribir",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDirty) "Aplicar" else "Escribir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
