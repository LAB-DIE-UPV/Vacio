package com.example.Vacio.bluetoothUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.Vacio.bluetoothPermision.hasBluetoothConnectPermission

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import com.example.Vacio.bluetoothAdapter.BluetoothAdapterHelper
import com.example.Vacio.bluetoothService.BluetoothService

/**
 * Obtiene de forma segura el nombre de un dispositivo Bluetooth.
 * En Android 12+ requiere permiso BLUETOOTH_CONNECT para acceder a device.name;
 * si no hay permiso, retorna la dirección MAC para evitar SecurityException.
 *
 * @param device Dispositivo Bluetooth
 * @param context Contexto de la aplicación para verificar permisos
 * @return Nombre del dispositivo o dirección si no está disponible o no hay permiso
 */
@SuppressLint("MissingPermission")
fun getDeviceNameSafely(device: BluetoothDevice, context: Context): String {
    return if (hasBluetoothConnectPermission(context)) {
        device.name ?: device.address
    } else {
        device.address
    }
}

/**
 * Maneja la acción de encender/apagar Bluetooth.
 * - En Android 12+ lanza solicitud de permiso si es necesario.
 * - Si toggled == true, inicia intent para activar Bluetooth.
 * - Si toggled == false, desconecta el servicio y desactiva el adaptador.
 *
 * @param toggled Estado al que se desea cambiar (true = encender, false = apagar)
 * @param context Contexto para lanzar intents y peticiones
 * @param bluetoothHelper Helper para controlar el adaptador Bluetooth
 * @param bluetoothService Servicio Bluetooth para desconectar socket antes de apagar
 * @param permissionLauncher Lanzador para solicitar permisos en runtime
 * @param enableBluetoothLauncher Callback para lanzar intent de activación
 * @param onMissingPermission Callback cuando no hay permiso para operar
 */
fun handleBluetoothToggleAction(
    toggled: Boolean,
    context: Context,
    bluetoothHelper: BluetoothAdapterHelper,
    bluetoothService: BluetoothService?, // ✅ añadido el servicio aquí
    permissionLauncher: ActivityResultLauncher<String>,
    enableBluetoothLauncher: (Intent) -> Unit,
    onMissingPermission: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !hasBluetoothConnectPermission(context)) {
        // Solicitar permiso y notificar
        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        onMissingPermission()
        return
    }

    if (toggled) {
        // Intent para encender Bluetooth si aún está apagado
        if (!bluetoothHelper.isBluetoothEnabled()) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // En Android 13+, abrir ajustes de Bluetooth en lugar de petición directa
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            } else {
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            }
            enableBluetoothLauncher(intent)
        }
    } else {
        // ✅ Cierra la conexión del servicio ANTES de desactivar el adaptador
        bluetoothService?.disconnectsocket()
        bluetoothHelper.disableBluetooth()
    }
}


/**
 * Intenta mostrar la lista de dispositivos emparejados.
 * - En Android 12+ verifica y solicita permiso BLUETOOTH_CONNECT.
 * - Llama onDevicesReady con la lista si hay permiso o en versiones previas.
 * - Llama onPermissionRejected si el usuario rechaza el permiso.
 *
 * @param context Contexto de la aplicación para verificar permisos
 * @param bluetoothHelper Helper para obtener dispositivos emparejados
 * @param permissionLauncher Lanzador para solicitud de permiso
 * @param onDevicesReady Callback con la lista de dispositivos (si permiso OK)
 * @param onPermissionRejected Callback si no se concede el permiso
 */
fun tryShowBondedDevicesWithPermission(
    context: Context,
    bluetoothHelper: BluetoothAdapterHelper,
    permissionLauncher: ActivityResultLauncher<String>,
    onDevicesReady: (List<BluetoothDevice>) -> Unit,
    onPermissionRejected: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // En Android 12+, verificar permiso BLUETOOTH_CONNECT
        if (hasBluetoothConnectPermission(context)) {
            onDevicesReady(bluetoothHelper.getBondedDevices())
        } else {
            // Solicitar permiso y notificar rechazo si no se concede
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            onPermissionRejected()
        }
    } else {
        // En versiones previas, no se requiere permiso para acceder a dispositivos emparejados
        onDevicesReady(bluetoothHelper.getBondedDevices())
    }
}


