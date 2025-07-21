package com.example.Vacio.bluetoothAdapter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Helper para interactuar con el adaptador Bluetooth de forma segura.
 * Verifica permisos y proporciona métodos útiles para otras capas de la app.
 *
 * @param context Contexto necesario para verificar permisos en Android 12+
 * @param bluetoothAdapter Instancia del adaptador Bluetooth del sistema
 */
class BluetoothAdapterHelper(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {

    /**
     * Verifica si el Bluetooth está activado en el dispositivo.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Devuelve la lista de dispositivos emparejados (bonded).
     * Verifica el permiso BLUETOOTH_CONNECT en Android 12+.
     *
     * @return Lista de dispositivos emparejados o vacía si no hay permiso.
     */
    fun getBondedDevices(): List<BluetoothDevice> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        }
    }

    /**
     * Intenta desactivar el Bluetooth.
     * En Android 13+ (TIRAMISU) ya no es posible desactivarlo desde código,
     * por lo que debe redirigirse al usuario a la configuración.
     *
     * Esta función también verifica el permiso requerido.
     */
    fun disableBluetooth() {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                try {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter?.disable()
                    Log.d("BluetoothAdapterHelper", "Bluetooth desactivado programáticamente.")
                } catch (e: SecurityException) {
                    Log.e("BluetoothAdapterHelper", "Permiso denegado para desactivar Bluetooth: ${e.message}")
                }
            } else {
                // En Android 13+, redirigir desde UI (ya lo manejas)
                Log.d("BluetoothAdapterHelper", "Android 13+: desactivación directa no permitida.")
            }
        } else {
            Log.w("BluetoothAdapterHelper", "No se tiene permiso para desactivar Bluetooth.")
        }
    }
}


