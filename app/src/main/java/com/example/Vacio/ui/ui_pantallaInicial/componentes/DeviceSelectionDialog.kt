package com.example.Vacio.ui.ui_pantallaInicial.componentes

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.collections.forEach



/**
 * Diálogo Compose para mostrar dispositivos Bluetooth emparejados y permitir la selección.
 *
 * Maneja permisos de Android 12+ para acceso a nombre de dispositivo;
 * si no hay permiso o falla, muestra la dirección MAC.
 *
 * @param bondedDevices Lista de dispositivos Bluetooth emparejados.
 * @param onDeviceSelected Lambda que recibe el dispositivo seleccionado.
 * @param onDismiss Lambda para cerrar el diálogo sin acción.
 */

@Composable
fun DeviceSelectionDialog(
    bondedDevices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    // Contexto de la Activity para revisar permisos y crear Toasts si fuera necesario
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona un dispositivo") },
        text = {
            Column {
                // Si no hay dispositivos emparejados, informar al usuario
                if (bondedDevices.isEmpty()) {
                    Text("No hay dispositivos emparejados. ¿Bluetooth Encendido?")
                } else {
                    // Itera la lista de dispositivos emparejados
                    bondedDevices.forEach { device ->
                        // ⚠️ Verificamos si tenemos permiso para acceder al nombre
                        val name = try {
                            // A partir de Android S (API 31) se requiere BLUETOOTH_CONNECT
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                device.address // sin permiso, mostramos solo dirección
                            } else {
                                device.name ?: device.address // con permiso, mostramos nombre
                            }
                        } catch (e: SecurityException) {
                            device.address // por si acaso salta excepción
                        }

                        TextButton(onClick = { onDeviceSelected(device) }) {
                            Text(name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Botón para cancelar y cerrar el diálogo
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}