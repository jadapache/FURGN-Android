package com.furgnbi.android.presentation.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.furgnbi.android.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showPassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("Configuración guardada correctamente")
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configuración del Sistema",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Parámetros de Red, Modbus PLC y Broker MQTT",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveConfig() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Guardar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Dispositivo
            ConfigSectionCard(title = "Identificación de la Unidad", icon = Icons.Default.LocalShipping) {
                OutlinedTextField(
                    value = state.deviceId,
                    onValueChange = { viewModel.updateDeviceId(it) },
                    label = { Text("Device ID (Placa / Unidad)") },
                    singleLine = true,
                    colors = configTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 2: Modbus TCP
            ConfigSectionCard(title = "Comunicación Modbus TCP (PLC MICRO 820)", icon = Icons.Default.Router) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.modbusHost,
                        onValueChange = { viewModel.updateModbusHost(it) },
                        label = { Text("Host / IP PLC") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = configTextFieldColors(),
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = state.modbusPort,
                        onValueChange = { viewModel.updateModbusPort(it) },
                        label = { Text("Puerto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = configTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.modbusUnitId,
                        onValueChange = { viewModel.updateModbusUnitId(it) },
                        label = { Text("Unit ID") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = configTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Broker MQTT
            ConfigSectionCard(title = "Broker EMQX / MQTT (QoS 1)", icon = Icons.Default.CloudSync) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.brokerUrl,
                        onValueChange = { viewModel.updateBrokerUrl(it) },
                        label = { Text("URL Broker (tcp://host:port o ssl://host:port)") },
                        singleLine = true,
                        colors = configTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.mqttUsername,
                            onValueChange = { viewModel.updateMqttUsername(it) },
                            label = { Text("Usuario MQTT") },
                            singleLine = true,
                            colors = configTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.mqttPassword,
                            onValueChange = { viewModel.updateMqttPassword(it) },
                            label = { Text("Contraseña MQTT") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar contraseña",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = configTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = state.mqttClientId,
                        onValueChange = { viewModel.updateMqttClientId(it) },
                        label = { Text("Client ID MQTT (Opcional)") },
                        singleLine = true,
                        colors = configTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 4: Intervalos de Muestreo y Publicación
            ConfigSectionCard(title = "Tiempos e Intervalos", icon = Icons.Default.Timer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.samplingIntervalMs,
                        onValueChange = { viewModel.updateSamplingInterval(it) },
                        label = { Text("Muestreo Modbus (ms)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = configTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.publishIntervalMs,
                        onValueChange = { viewModel.updatePublishInterval(it) },
                        label = { Text("Publicación MQTT (ms)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = configTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
fun configTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BackgroundDark,
    unfocusedContainerColor = BackgroundDark,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary
)
