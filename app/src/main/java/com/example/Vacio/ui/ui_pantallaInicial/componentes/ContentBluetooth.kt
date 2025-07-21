package com.example.Vacio.ui.ui_pantallaInicial.componentes

// BluetoothContent: Composable que muestra el estado de conexión Bluetooth,
// instrucciones de ensayo, valores medidos, gráficos y controles de navegación
import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.Vacio.bluetoothService.BluetoothService

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BluetoothContent(
    navController: NavHostController,
    bluetoothService: BluetoothService?,
    connectionStatus: Boolean?,          // Estado de conexión: true conectado, false desconectado, null Bluetooth apagado
    modifier: Modifier = Modifier        // Modificador para posicionamiento/padding
) {
    // Contenedor principal vertical con padding
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // ---------------------------------------------
        // Sección superior: estado de conexión y botón de ayuda
        // ---------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Texto dinámico con iconos según estado
            Text(
                text = "Estado conexión: " + when (connectionStatus) {
                    true -> "Conectado ✅"
                    false -> "No conectado ❌"
                    null -> "Bluetooth apagado 🚫"
                },
                style = MaterialTheme.typography.titleSmall
            )
        }

        //AÑADIR CONTENIDO


    }
}

