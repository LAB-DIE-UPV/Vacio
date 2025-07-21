package com.example.Vacio.bluetoothPermision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Verifica si la app tiene permiso para acceder a funciones de Bluetooth seguras.
 *
 * A partir de Android 12 (API 31), se requiere el permiso BLUETOOTH_CONNECT
 * para acceder a ciertos métodos del BluetoothAdapter y BluetoothDevice.
 *
 * @param context El contexto usado para consultar permisos.
 * @return true si se tiene permiso o si no es necesario (Android < 12).
 */
fun hasBluetoothConnectPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
