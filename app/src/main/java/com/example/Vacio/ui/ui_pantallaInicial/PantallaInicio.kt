package com.example.Vacio.ui.ui_pantallaInicial

// Pantalla principal de la app: gestiona conexión BLE, muestra datos y genera informes PDF
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.Vacio.ui.ui_pantallaInicial.componentes.BluetoothContent
import com.example.Vacio.bluetoothAdapter.BluetoothAdapterHelper
import com.example.Vacio.bluetoothService.BluetoothService
import com.example.Vacio.bluetoothUtils.getDeviceNameSafely
import com.example.Vacio.bluetoothUtils.tryShowBondedDevicesWithPermission
import com.example.Vacio.bluetoothUtils.handleBluetoothToggleAction
import com.example.Vacio.ui.ui_pantallaInicial.componentes.BluetoothTopBar
import com.example.Vacio.ui.ui_pantallaInicial.componentes.DeviceSelectionDialog
import com.example.Vacio.ui.ui_pantallaInicial.componentes.DialogoSeleccionPdf
import com.example.Vacio.ui.ui_pantallaInicial.componentes.capturarVistaAltaCalidadSinBarras
import com.example.Vacio.ui.ui_pantallaInicial.componentes.generarInformePdfConNombre
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    navController: NavHostController,
    bluetoothService: BluetoothService?,        // Servicio Bluetooth enlazado
    bluetoothHelper: BluetoothAdapterHelper,     // Helper para gestionar Bluetooth
) {
    // Contexto de la Activity para acceder a recursos y lanzar Intents
    val context = LocalContext.current       // Contexto de la Activity
    // Scope para lanzar coroutines desde Compose
    val coroutineScope = rememberCoroutineScope()   // Scope para lanzar coroutines
    // Estado para gestionar snackbars (mensajes temporales en la parte inferior)
    val snackbarHostState = remember { SnackbarHostState() }  // Estado del Snackbar
    // Controlador para personalizar colores de la barra de estado / navegación
    val systemUiController = rememberSystemUiController()      // Control del sistema UI


// --------------------
    // Estados para diálogos y conexión
    // --------------------
    // Muestra el diálogo de selección de dispositivo
    var showDialog by remember { mutableStateOf(false) }
    // Lista de dispositivos emparejados disponibles
    var bondedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    // Estado de conexión: true = conectado, false = desconectado, null = Bluetooth apagado
    var connectionStatus by remember { mutableStateOf<Boolean?>(null) }
    // Estado de si el adaptador Bluetooth está encendido
    var isBluetoothOn by remember { mutableStateOf(bluetoothHelper.isBluetoothEnabled()) }

    // --------------------
    // Flujos de datos de los canales (200 muestras cada uno)
    // --------------------
    // Observa LiveData del servicio y convierte a estado Compose
    val canal1: List<Int> by bluetoothService
        ?.canal1LiveData
        ?.observeAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val canal2: List<Int> by bluetoothService
        ?.canal2LiveData
        ?.observeAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val canal3: List<Int> by bluetoothService
        ?.canal3LiveData
        ?.observeAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val canal4: List<Int> by bluetoothService
        ?.canal4LiveData
        ?.observeAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    val canal5: List<Int> by bluetoothService
        ?.canal5LiveData
        ?.observeAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }


    // --------------------
    // Estados para control de diálogos PDF
    // --------------------
    var showPdfDialog by remember { mutableStateOf(false) }     // Muestra lista de PDFs
    var showNombreDialog by remember { mutableStateOf(false) }  // Dialogo para introducir nombre de PDF
    var nombrePersonalizado by remember { mutableStateOf("") }  // Nombre ingresado por el usuario


    // --------------------
    // Observación periódica de estado Bluetooth y socket
    // --------------------
    LaunchedEffect(bluetoothHelper, bluetoothService) {
        while (true) {
            // Verifica estado del adaptador y del socket
            val adapterConnected = bluetoothHelper.isBluetoothEnabled()
            val socketConnected = bluetoothService?.isConnected() == true
            isBluetoothOn = adapterConnected
            connectionStatus = if (adapterConnected) {
                socketConnected
            } else {
                null
            }
            delay(500)    // Comprobación cada medio segundo
        }
    }

    // --------------------
    // Al conectarse, envía comando INICIAR y muestra snackbar
    // --------------------
    LaunchedEffect(connectionStatus) {
        /*
        if (connectionStatus == true) {
            delay(1000)  // Esperar un segundo tras conectar
            bluetoothService?.sendMessage("INICIAR")
            // Evita duplicar el snackbar
            if (bluetoothService?.mensajeIniciadoGlobal == false) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("INICIANDO MEDICION")
                }
                bluetoothService.mensajeIniciadoGlobal = true
            }
        }
         */
    }

    // --------------------
    // Configura el color de la barra de estado (solo una vez)
    // --------------------
    SideEffect {
        systemUiController.setStatusBarColor(Color.White, darkIcons = true)
    }

    // --------------------
    // Lanzadores para permisos y activación de Bluetooth
    // --------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Permiso de Bluetooth rechazado")
            }
        }
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!bluetoothHelper.isBluetoothEnabled()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Bluetooth no activado")
            }
        }
    }

    // --------------------
    // Estructura general de la pantalla con Scaffold
    // -------------------
    Scaffold(
        topBar = {
            BluetoothTopBar(
                // Barra superior con controles de Bluetooth, PDF y navegación a calibración
                isBluetoothOn = isBluetoothOn,
                onDeviceButtonClick = {
                    // Mostrar diálogo de dispositivos emparejados con permisos
                    tryShowBondedDevicesWithPermission(
                        context,
                        bluetoothHelper,
                        permissionLauncher,
                        onDevicesReady = {
                            bondedDevices = it
                            showDialog = true
                        },
                        onPermissionRejected = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Se necesita permiso para ver dispositivos emparejados")
                            }
                        }
                    )
                },
                onToggleBluetooth = { toggled ->
                    // Encender/apagar Bluetooth según acción del usuario
                    handleBluetoothToggleAction(
                        toggled,
                        context,
                        bluetoothHelper,
                        bluetoothService,
                        permissionLauncher,
                        enableBluetoothLauncher::launch,
                        onMissingPermission = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Permiso necesario para cambiar el estado del Bluetooth")
                            }
                        }
                    )
                },
                onGeneratePdfClick = { showNombreDialog = true },  // Abrir diálogo de nombre de PDF
                onViewPdfClick = { showPdfDialog = true },         // Mostrar lista de PDFs guardados
                onCalibracionClick = { navController.navigate("calibracion") }
            )
            // Dialogos de selección de PDF y nombre personalizado
            if (showPdfDialog) {
                DialogoSeleccionPdf(
                    context = context,
                    onDismiss = { showPdfDialog = false }
                )
            }
            // Diálogo para ingresar nombre del nuevo informe
            if (showNombreDialog) {
                AlertDialog(
                    onDismissRequest = { showNombreDialog = false },
                    title = { Text("Generar PDF") },
                    text = {
                        Column {
                            Text("Introduce un nombre descriptivo para el archivo:")
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = nombrePersonalizado,
                                onValueChange = { nombrePersonalizado = it },
                                singleLine = true,
                                placeholder = { Text("Ej: Ensayo1_JorgeyDiego") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showNombreDialog = false
                            // Capturar screenshot sin barras y generar informe PDF
                            val activity = context as? Activity
                            val rootView = activity?.window?.decorView?.rootView
                            val density = context.resources.displayMetrics.density
                            val topBarHeightPx = (64 * density).toInt()
                            val bottomBarHeightPx = (48 * density).toInt()
                            val screenshot = rootView?.let {
                                capturarVistaAltaCalidadSinBarras(it, context, topBarHeightPx, bottomBarHeightPx)
                            }
                            generarInformePdfConNombre(
                                context = context,
                                nombreBase = nombrePersonalizado,
                                screenshot = screenshot
                            )
                        }) { Text("Generar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNombreDialog = false }) { Text("Cancelar") }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Contenido principal con datos y estado de conexión
        BluetoothContent(
            navController = navController,
            bluetoothService= bluetoothService,
            connectionStatus = connectionStatus,
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        )
    }

    // -----------------------------------------
    // Diálogo flotante de selección de dispositivo
    // -----------------------------------------
    if (showDialog) {
        DeviceSelectionDialog(
            bondedDevices = bondedDevices,
            onDeviceSelected = { device ->
                showDialog = false
                if (bluetoothService == null) {
                    Log.e("PantallaInicio", "Servicio Bluetooth aún no enlazado")
                    Toast.makeText(context, "Servicio aún no está listo", Toast.LENGTH_SHORT).show()
                } else {
                    // Conectar al dispositivo seleccionado
                    val name = getDeviceNameSafely(device, context)
                    Log.d("PantallaInicio", "Conectando a $name")
                    bluetoothService.connectToDevice(device)
                    Toast.makeText(context, "Conectando a $name...", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showDialog = false }
        )
    }
}
