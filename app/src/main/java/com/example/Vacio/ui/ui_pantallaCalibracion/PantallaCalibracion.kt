package com.example.Vacio.ui.ui_pantallaCalibracion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.Vacio.CalcularUtils.tablaCalibracion
import com.example.Vacio.CalcularUtils.tablaPorDefecto
import com.example.Vacio.bluetoothService.BluetoothService
import com.example.Vacio.ConfiguracionPrefs.ConfiguracionPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon

/**
 * Pantalla de calibración donde el usuario:
 * 1. Introduce valores de referencia manualmente.
 * 2. Ve lecturas crudas en tiempo real del ESP32.
 * 3. Calcula y guarda factores de escala.
 * 4. Envía factores al ESP32.
 * 5. Compara factores user vs ESP32.
 * 6. Visualiza gráficas de fase 1 y 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCalibracion(
    navController: NavHostController,
    bluetoothService: BluetoothService,
) {
    // Instancia de SharedPreferences para obtener valores guardados
    val configPrefs = ConfiguracionPrefs(LocalContext.current)
    // Scope para lanzar coroutines (snackbar y retrasos)
    val scope = rememberCoroutineScope()

    // ------------------------------
    // Entradas de usuario (factor manual)
    // ------------------------------
    // Inicializamos los valores de texto con los valores por defecto guardados

    var userTensionA by remember { mutableStateOf(configPrefs.defaultUserTensionA.toString()) }
    var userTensionB by remember { mutableStateOf(configPrefs.defaultUserTensionB.toString()) }
    var userTensionC by remember { mutableStateOf(configPrefs.defaultUserTensionC.toString()) }
    var userCurrentA by remember { mutableStateOf(configPrefs.defaultUserCurrentA.toString()) }
    var userCurrentB by remember { mutableStateOf(configPrefs.defaultUserCurrentB.toString()) }


    // ------------------------------
    // Mediciones en crudo (LiveData desde BluetoothService)
    // ------------------------------

    val rawTensionA = bluetoothService.rmsCanal1.observeAsState(0f)
    val rawTensionB = bluetoothService.rmsCanal2.observeAsState(0f)
    val rawTensionC = bluetoothService.rmsCanal3.observeAsState(0f)
    val rawCorrienteA = bluetoothService.rmsCanal4.observeAsState(0f)
    val rawCorrienteB = bluetoothService.rmsCanal5.observeAsState(0f)



    // ------------------------------
    // Factores actuales y de usuario
    // ------------------------------
    // Inicializa factores con valores guardados en preferencias

    var factorTensionA by remember { mutableStateOf(configPrefs.factorTensionA) }
    var factorTensionB by remember { mutableStateOf(configPrefs.factorTensionB) }
    var factorTensionC by remember { mutableStateOf(configPrefs.factorTensionC) }
    var factorCorrienteA by remember { mutableStateOf(configPrefs.factorCorrienteA) }
    var factorCorrienteB by remember { mutableStateOf(configPrefs.factorCorrienteB) }

    // Estado para mostrar snackbars de feedback
    val snackbarHostState = remember { SnackbarHostState() }


    // ------------------------------
    // Factores recibidos del ESP32 (LiveData)
    // ------------------------------

    var factorEsp32TensionA by remember { mutableStateOf(configPrefs.esp32FactorTensionA) }
    var factorEsp32TensionB by remember { mutableStateOf(configPrefs.esp32FactorTensionB) }
    var factorEsp32TensionC by remember { mutableStateOf(configPrefs.esp32FactorTensionC) }
    var factorEsp32CorrienteA by remember { mutableStateOf(configPrefs.esp32FactorCorrienteA) }
    var factorEsp32CorrienteB by remember { mutableStateOf(configPrefs.esp32FactorCorrienteB) }



    // Ciclo para actualizar los factores del ESP32 si cambian en SharedPreferences
    LaunchedEffect(Unit) {
        while (true) {
            // Lee nuevos valores de SharedPreferences
            val newTA = configPrefs.esp32FactorTensionA
            val newTB = configPrefs.esp32FactorTensionB
            val newTC = configPrefs.esp32FactorTensionC
            val newCorrA = configPrefs.esp32FactorCorrienteA
            val newCorrB = configPrefs.esp32FactorCorrienteB
            // Actualiza si hay cambios
            if (factorEsp32TensionA != newTA) factorEsp32TensionA = newTA
            if (factorEsp32TensionB != newTB) factorEsp32TensionB = newTB
            if (factorEsp32TensionC != newTC) factorEsp32TensionC = newTC
            if (factorEsp32CorrienteA != newCorrA) factorEsp32CorrienteA = newCorrA
            if (factorEsp32CorrienteB != newCorrB) factorEsp32CorrienteB = newCorrB

            delay(500) // revisa cada medio segundo (ajusta si quieres)
        }
    }

    val scrollState = rememberScrollState()

    // Estado para habilitar uso de tabla por defecto vs ESP32
    val estaUsandoTablaPorDefecto by bluetoothService.usarTablaPorDefectoLive.observeAsState(false)

    // ---------------------------------------------
    // UI principal con Scaffold: barra y contenido scrollable
    // ---------------------------------------------
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Calibración") },
                actions = {
                    // Botón para alternar entre tabla por defecto y tabla del ESP32
                    Button(
                        onClick = {
                            val nuevoEstado = !estaUsandoTablaPorDefecto
                            bluetoothService.usarTablaPorDefectoLive.value = nuevoEstado

                            if (nuevoEstado) {
                                // Si se activa la tabla por defecto
                                tablaCalibracion = tablaPorDefecto.toList()
                            } else {
                                // Si se vuelve a usar la tabla del ESP32, cargamos la última recibida si existe
                                bluetoothService.ultimaTablaRecibida?.let {
                                    tablaCalibracion = it
                                }
                            }
                        }
                        ,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (estaUsandoTablaPorDefecto) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (estaUsandoTablaPorDefecto) Icons.Default.Restore else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (estaUsandoTablaPorDefecto) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (estaUsandoTablaPorDefecto) "Tabla por defecto activa"
                            else "Usando tabla del ESP32",
                            color = if (estaUsandoTablaPorDefecto) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(20.dp))
                    // Botón para volver a la pantalla anterior
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Volver")
                    }
                }
            )

        }
    ) { innerPadding ->
        // Contenido scrollable con formularios y tablas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            //Tabla de Mediciones por Fase
            Text("Mediciones por Fase", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                TableCell("Parámetro", 1f, true)
                TableCell("User", 1f, true)
                TableCell("Auto(User)", 1f, true)
            }

            Spacer(Modifier.height(8.dp))

            listOf(
                Triple("Tensión A", userTensionA, rawTensionA.value),
                Triple("Tensión B", userTensionB, rawTensionB.value),
                Triple("Tensión C", userTensionC, rawTensionC.value),
                Triple("Corriente A", userCurrentA, rawCorrienteA.value),
                Triple("Corriente B", userCurrentB, rawCorrienteB.value)

            ).forEach { (label, userValue, rawValue) ->
                // Selecciona factor correspondiente
                val escala = when (label) {
                    "Corriente A" -> factorCorrienteA
                    "Corriente B" -> factorCorrienteB
                    "Tensión A" -> factorTensionA
                    "Tensión B" -> factorTensionB
                    else -> factorTensionC
                }
                val valorCorregido = rawValue * escala

                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    TableCell(label, 1f)
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = when (label) {
                                "Corriente A" -> userCurrentA
                                "Corriente B" -> userCurrentB
                                "Tensión A" -> userTensionA
                                "Tensión B" -> userTensionB
                                else -> userTensionC
                            },
                            onValueChange = {
                                when (label) {
                                    "Corriente A" -> userCurrentA = it
                                    "Corriente B" -> userCurrentB = it
                                    "Tensión A" -> userTensionA = it
                                    "Tensión B" -> userTensionB = it
                                    else -> userTensionC = it
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TableCell("%.4f".format(valorCorregido), 1f) // 👉 ahora con 4 decimales y corregido
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {

                Button(
                    onClick = {

                        val userTA = userTensionA.toFloatOrNull() ?: 1f
                        val userTB = userTensionB.toFloatOrNull() ?: 1f
                        val userTC = userTensionC.toFloatOrNull() ?: 1f
                        val userCorrA = userCurrentA.toFloatOrNull() ?: 1f
                        val userCorrB = userCurrentB.toFloatOrNull() ?: 1f

                        // Calcula nuevos factores a partir de user/raw
                        factorTensionA = if (rawTensionA.value > 0) userTA / rawTensionA.value else 1f
                        factorTensionB = if (rawTensionB.value > 0) userTB / rawTensionB.value else 1f
                        factorTensionC = if (rawTensionC.value > 0) userTC / rawTensionC.value else 1f
                        factorCorrienteA = if (rawCorrienteA.value > 0) userCorrA / rawCorrienteA.value else 1f
                        factorCorrienteB = if (rawCorrienteB.value > 0) userCorrB / rawCorrienteB.value else 1f

                        // 👉 GUARDAR en SharedPreferences

                        configPrefs.factorTensionA = factorTensionA
                        configPrefs.factorTensionB = factorTensionB
                        configPrefs.factorTensionC = factorTensionC
                        configPrefs.factorCorrienteA = factorCorrienteA
                        configPrefs.factorCorrienteB = factorCorrienteB

                        scope.launch {
                            snackbarHostState.showSnackbar("Factores guardados")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar Factores")
                }


                Spacer(Modifier.width(8.dp))

                //Botón Resetear Factores al ESP32
                Button(
                    onClick = {
                        factorTensionA = 1f
                        factorTensionB = 1f
                        factorTensionC = 1f
                        factorCorrienteA = 1f
                        factorCorrienteB = 1f

                        userTensionA = configPrefs.defaultUserTensionA.toString()
                        userTensionB = configPrefs.defaultUserTensionB.toString()
                        userTensionC = configPrefs.defaultUserTensionC.toString()
                        userCurrentA = configPrefs.defaultUserCurrentA.toString()
                        userCurrentB = configPrefs.defaultUserCurrentA.toString()

                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Resetear Valores") }
            }

            Spacer(Modifier.height(8.dp))
            //Botón Enviar Factores al ESP32
            Button(
                onClick = {
                    val mensaje = "SET_FACTORS:$factorTensionA,$factorTensionB,$factorTensionC,$factorCorrienteA,$factorCorrienteB"

                    bluetoothService.sendMessage(mensaje)
                    scope.launch {
                        snackbarHostState.showSnackbar("Factores enviados al ESP32")
                        delay(500) // espera 1 segundo
                        bluetoothService.sendMessage("INICIAR")
                        snackbarHostState.showSnackbar("INICIANDO MEDICIÓN")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Factores a ESP32")
            }

            Spacer(Modifier.height(32.dp))

            // Comparación de factores: user vs ESP32
            Text("Comparación de Factores de ESCALA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {
                TableCell("Fase", 1f, true)
                TableCell("User", 1f, true)
                TableCell("ESP32", 1f, true)
                TableCell("Diferencia", 1f, true)
            }

            Spacer(Modifier.height(8.dp))

            listOf<Triple<String, Float, Float>>(
                Triple("Tensión A", factorTensionA, factorEsp32TensionA),
                Triple("Tensión B", factorTensionB, factorEsp32TensionB),
                Triple("Tensión C", factorTensionC, factorEsp32TensionC),
                Triple("Corriente A", factorCorrienteA, factorEsp32CorrienteA),
                Triple("Corriente B", factorCorrienteB, factorEsp32CorrienteB)

            ).forEach { (label, userFactor, esp32Factor) ->
                val diffPercent = if (esp32Factor != 0f) ((userFactor - esp32Factor) / esp32Factor) * 100f else 0f
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    TableCell(label, 1f)
                    TableCell("%.2f".format(userFactor), 1f)
                    TableCell("%.2f".format(esp32Factor), 1f)
                    TableCell("%.2f%%".format(diffPercent), 1f)
                }
            }

            Spacer(Modifier.height(32.dp))

            //GRAFICA ADC ESP32

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)          // 2 × 250 dp (o ajusta a tu gusto)
            ) {
                RawCalibrationChart(
                    title  = "ADC ESP32 FASE A",
                    series = listOf(
                        "Tensión A ADC"   to bluetoothService.canal1LiveData.observeAsState(emptyList()).value,
                        "Corriente A ADC" to bluetoothService.canal4LiveData.observeAsState(emptyList()).value
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)      // alto de la primera gráfica
                )

                Spacer(Modifier.height(32.dp))

                RawCalibrationChart(
                    title  = "ADC ESP32 FASE B",
                    series = listOf(
                        "Tensión B ADC"   to bluetoothService.canal2LiveData.observeAsState(emptyList()).value,
                        "Corriente B ADC" to bluetoothService.canal5LiveData.observeAsState(emptyList()).value
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)      // alto de la segunda gráfica
                )
            }

        }
    }
}

/**
 * Composable auxiliar para crear celdas de tabla en las filas.
 * @param text Texto a mostrar.
 * @param cellWeight Peso para distribuir espacio.
 * @param isHeader Si es true, aplica estilo de encabezado.
 */
@Composable
fun RowScope.TableCell(text: String, cellWeight: Float, isHeader: Boolean = false) {
    Text(
        text = text,
        style = if (isHeader) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        else MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(cellWeight).padding(4.dp)
    )
}
