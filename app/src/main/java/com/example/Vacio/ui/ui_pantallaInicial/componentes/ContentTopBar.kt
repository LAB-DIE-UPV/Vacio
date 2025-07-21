package com.example.Vacio.ui.ui_pantallaInicial.componentes

// BluetoothTopBar: Barra superior con botones de control, generación de PDF y acceso restringido a calibración
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Tune

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothTopBar(
    isBluetoothOn: Boolean,                      // Estado del Bluetooth (encendido/apagado)
    onDeviceButtonClick: () -> Unit,            // Callback al presionar el botón de dispositivos
    onToggleBluetooth: (Boolean) -> Unit,       // Callback al cambiar el toggle de Bluetooth
    onGeneratePdfClick: () -> Unit,             // Callback al generar un nuevo PDF
    onViewPdfClick: () -> Unit,                 // Callback al ver PDFs guardados
    onCalibracionClick: () -> Unit             // Callback tras introducir contraseña admin
) {
    // Estado para mostrar/ocultar el diálogo de contraseña de administrador
    var showPasswordDialog by remember { mutableStateOf(false) }
    // Estado para almacenar la entrada de la contraseña
    var passwordInput by remember { mutableStateOf("") }
    // Contraseña fija (podría obtenerse de un recurso seguro en producción)
    val adminPassword = "1234"


    // ---------------------------------------------
    // Diálogo de contraseña de administrador
    // ---------------------------------------------

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Contraseña de Administrador") },
            text = {
                // Campo de texto para introducir la contraseña
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Introduce la contraseña") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Comprueba la contraseña introducida
                        if (passwordInput == adminPassword) {
                            // Si la contraseña es correcta, cerramos y ejecutamos acción de calibración
                            showPasswordDialog = false
                            passwordInput = ""
                            onCalibracionClick()
                        } else {
                            // Si falla, borra el input (se puede añadir mensaje de error)
                            passwordInput = ""  // Limpiamos input (opcional: se podría mostrar un error)
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ---------------------------------------------
    // Barra superior con título e iconos de acción
    // ---------------------------------------------
    TopAppBar(
        title = { Text("Inicio") },
        actions = {
            // Botón para calibración: intercepta click para pedir contraseña
            IconButton(onClick = { showPasswordDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Ir a Calibración",
                    tint = Color.White
                )
            }

            // Botón para ver PDFs guardados
            IconButton(onClick = onViewPdfClick) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "Ver PDF",
                    tint = Color.White
                )
            }

            // Botón para generar un nuevo PDF
            IconButton(onClick = onGeneratePdfClick) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = "Generar PDF",
                    tint = Color.White
                )
            }

            // Botón para mostrar dispositivos disponibles
            IconButton(onClick = onDeviceButtonClick) {
                Icon(
                    Icons.Filled.AccountTree,
                    contentDescription = "Conectar a dispositivo",
                    tint = Color.White
                )
            }

            // Toggle circular para encender/apagar Bluetooth
            IconToggleButton(
                checked = isBluetoothOn,
                onCheckedChange = onToggleBluetooth,
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (isBluetoothOn) MaterialTheme.colorScheme.secondaryContainer
                        else Color.LightGray
                    )
            ) {
                Icon(
                    imageVector = if (isBluetoothOn) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = "Bluetooth Toggle",
                    tint = if (isBluetoothOn) MaterialTheme.colorScheme.onSecondaryContainer else Color.DarkGray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}