
package com.example.Vacio

// MainActivity gestiona la UI con Jetpack Compose y enlaza el servicio Bluetooth
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.Vacio.bluetoothAdapter.BluetoothAdapterHelper
import com.example.Vacio.bluetoothService.BluetoothService
import com.example.Vacio.ui.PantallaDeCarga.SimpleSplash
import com.example.Vacio.ui.ui_pantallaCalibracion.PantallaCalibracion
import com.example.Vacio.ui.ui_pantallaInicial.PantallaInicio
import com.example.osci_2.ui.ui_pantallaSegunda.PantallaSegunda
import kotlinx.coroutines.delay


// Clase principal de la app. Se encarga de enlazar el servicio Bluetooth y gestionar las pantallas con Compose
class MainActivity : ComponentActivity() {

    // Estado para guardar la referencia al servicio Bluetooth cuando esté enlazado
    private val bluetoothServiceState = mutableStateOf<BluetoothService?>(null)

    // Objeto ServiceConnection para manejar la conexión/desconexión del servicio
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // Al conectar, obtenemos el servicio desde el binder y actualizamos el estado
            bluetoothServiceState.value = (binder as BluetoothService.LocalBinder).getService()
            Log.d("MainActivity", "✅ Servicio enlazado correctamente")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            // Al desconectar, limpiamos la referencia para evitar fugas
            bluetoothServiceState.value = null
            Log.d("MainActivity", "🔌 Servicio desenlazado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permitir que el contenido Compose se dibuje bajo las barras de sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Controlador de navegación para Compose
            val navController = rememberNavController()
            // Obtenemos la instancia del servicio desde el estado
            val service by bluetoothServiceState
            // Variable para controlar cuándo termina el splash screen
            var splashDone by remember { mutableStateOf(false) }

            // Lanzar y enlazar el servicio Bluetooth justo al iniciar la UI
            LaunchedEffect(Unit) {
                Intent(this@MainActivity, BluetoothService::class.java).also { intent ->
                    startService(intent) // Iniciar servicio en background
                    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) // Enlazar al servicio
                }
            }

            // Esperar que el servicio esté listo y mantener el splash al menos 2 segundos
            LaunchedEffect(service) {
                if (service != null) {
                    delay(2000)  // Duración mínima del SimpleSplash
                    splashDone = true
                }
            }

            // Mostrar pantalla de carga mientras no haya terminado el splash
            if (!splashDone) {
                SimpleSplash(
                    message = "Iniciando servicio Bluetooth…",
                    totalDurationMs = 2000L
                ) { /* onTimeout no necesario */ }
            } else {
                // Configurar rutas de navegación una vez finalizado el splash
                NavHost(navController, startDestination = "inicio") {
                    composable("inicio") {
                        val bluetoothManager =
                            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val bluetoothHelper = remember {
                            BluetoothAdapterHelper(this@MainActivity, bluetoothManager.adapter)
                        }
                        PantallaInicio(navController, service!!, bluetoothHelper)
                    }
                    composable("segunda") {
                        PantallaSegunda(navController, service!!)
                    }
                    composable("calibracion") {
                        PantallaCalibracion(navController, service!!)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desenlazar el servicio al destruir la actividad para evitar fugas
        unbindService(serviceConnection)
    }
}
