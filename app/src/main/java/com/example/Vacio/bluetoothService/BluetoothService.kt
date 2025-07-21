package com.example.Vacio.bluetoothService

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.Vacio.ConfiguracionPrefs.ConfiguracionPrefs
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.Executors


/**
 * Servicio en background para gestionar conexión Bluetooth SPP con un dispositivo ESP32.
 * Proporciona métodos para conectar, enviar/recibir datos y exponer LiveData con los canales.
 */
class BluetoothService : Service() {

    // Binder local para permitir que otras actividades se conecten a este servicio
    private val binder = LocalBinder()

    // Socket Bluetooth
    private var bluetoothSocket: BluetoothSocket? = null

    // UUID estándar para el perfil SPP (Serial Port Profile)
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Ejecutores para manejar envío y recepción en hilos separados
    private val sendExecutor = Executors.newSingleThreadExecutor()
    private val receiveExecutor = Executors.newSingleThreadExecutor()

    // LiveData para el estado de conexión
    val connectionStatusLiveData = MutableLiveData<Boolean>()

    // LiveData para los diferentes canales de datos
    val canal1LiveData = MutableLiveData<List<Int>>()
    val canal2LiveData = MutableLiveData<List<Int>>()
    val canal3LiveData = MutableLiveData<List<Int>>()
    val canal4LiveData = MutableLiveData<List<Int>>()
    val canal5LiveData = MutableLiveData<List<Int>>() // Canal 5 deshabilitado en esta app

    // Bandera para evitar múltiples mensajes INICIAR
    var mensajeIniciadoGlobal = false

    // LiveData para valores RMS de cada canal
    val rmsCanal1 = MutableLiveData<Float>()
    val rmsCanal2 = MutableLiveData<Float>()
    val rmsCanal3 = MutableLiveData<Float>()
    val rmsCanal4 = MutableLiveData<Float>()
    val rmsCanal5 = MutableLiveData<Float>()

    // LiveData para mensajes de depuración
    val debugMessageLiveData = MutableLiveData<String>()

    // Acumulador temporal de línea a línea para tabla de calibración recibida
    private var acumuladorTabla = StringBuilder()
    private var recibiendoTabla = false
    var ultimaTablaRecibida: List<Pair<Int, Double>>? = null
    val usarTablaPorDefectoLive = MutableLiveData(false)

    /**
     * Binder interno que expone el servicio a clientes enlazados.
     */
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    // Retorna el binder cuando el servicio es enlazado
    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Verifica permiso BLUETOOTH_CONNECT en Android 12+.
     */
    private fun hasBluetoothConnectPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * @return true si el socket está conectado
     */
    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true

    /**
     * Inicia conexión RFCOMM insegura con el dispositivo.
     * Se ejecuta en sendExecutor para no bloquear UI.
     */
    fun connectToDevice(device: BluetoothDevice) {
        // Verificar permiso
        if (!hasBluetoothConnectPermission()) {
            Log.e("BluetoothService", "Permiso BLUETOOTH_CONNECT no concedido.")
            connectionStatusLiveData.postValue(false)
            return
        }
        // Si ya conectado, actualizar LiveData y salir
        if (bluetoothSocket?.isConnected == true) {
            Log.d("BluetoothService", "Ya existe una conexión activa.")
            connectionStatusLiveData.postValue(true)
            return
        }

        // Ejecuta conexión en un hilo separado
        sendExecutor.execute {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(appUuid)
                bluetoothSocket?.let { socket ->
                    try {
                        socket.connect() // Esta llamada es bloqueante
                        if (socket.isConnected && socket.remoteDevice != null) {
                            startReceivingMessages()
                            Log.d("BluetoothService", "Conexión establecida con ${device.name}")
                            connectionStatusLiveData.postValue(true)
                        } else {
                            Log.e("BluetoothService", "Fallo en validación: socket no conectado.")
                            connectionStatusLiveData.postValue(false)
                        }
                    } catch (e: SecurityException) {
                        Log.e("BluetoothService", "Security exception al conectar: ${e.message}")
                        connectionStatusLiveData.postValue(false)
                    } catch (e: IOException) {
                        Log.e("BluetoothService", "Error IO al conectar: ${e.message}")
                        connectionStatusLiveData.postValue(false)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("BluetoothService", "Security exception al crear socket: ${e.message}")
                connectionStatusLiveData.postValue(false)
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error IO al crear socket: ${e.message}")
                connectionStatusLiveData.postValue(false)
            }
        }
    }

    /**
     * Envía un mensaje de texto terminado en '\n' al dispositivo.
     */
    fun sendMessage(message: String) {
        sendExecutor.execute {
            try {
                bluetoothSocket?.outputStream?.apply {
                    write("$message\n".toByteArray())
                    flush()
                }
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error al enviar mensaje: ${e.message}")
            }
        }
    }

    /**
     * Inicia bucle de recepción de datos desde inputStream.
     * Soporta encabezados '#1'..'#5' para datos binarios (400 bytes)
     * y líneas de texto para comandos (FACTORES, CALIB_TABLE).
     */
    private fun startReceivingMessages() {
        receiveExecutor.execute {
            val input = bluetoothSocket!!.inputStream
            val buffer = ByteArray(1024)
            var currentHeader: String? = null
            val fullBuffer = mutableListOf<Byte>()

            try {
                while (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) continue
                    var offset = 0
                    // Procesar cada byte
                    while (offset < bytesRead) {
                        if (currentHeader == null) {
                            // Detectar inicio de bloque binario (‘#1’..‘#5’)
                            if (bytesRead - offset >= 2 && buffer[offset].toInt().toChar() == '#') {
                                currentHeader = String(buffer, offset, 2, Charsets.UTF_8)
                                offset += 2
                                fullBuffer.clear()
                            } else {
                                // Lee texto normal terminado en '\n'
                                val sb = StringBuilder()
                                while (offset < bytesRead && buffer[offset] != '\n'.code.toByte()) {
                                    sb.append(buffer[offset].toInt().toChar())
                                    offset++
                                }
                                offset++ // omitir '\n'
                                val textoRecibido = sb.toString().trim()
                                if (textoRecibido.isNotEmpty()) {
                                    when {
                                        // FACTORES: mensaje con 5 valores para configurar escala
                                        textoRecibido.startsWith("FACTORES:") -> {
                                            guardarFactoresEnPreferencias(applicationContext, textoRecibido)
                                            debugMessageLiveData.postValue("Factores recibidos y guardados: $textoRecibido")
                                        }
                                        // CALIB_TABLE: inicio de envío de líneas de calibración
                                        textoRecibido.startsWith("CALIB_TABLE:") -> {
                                            if (usarTablaPorDefectoLive.value != true) {
                                                acumuladorTabla.clear()
                                                acumuladorTabla.append(textoRecibido.removePrefix("CALIB_TABLE:"))
                                                recibiendoTabla = true
                                            } else {
                                                Log.i("TablaCalibracion", "Ignorada porque está activada la tabla por defecto")
                                            }
                                        }

                                        // END_TABLE: fin de datos de calibración
                                        textoRecibido == "END_TABLE" && recibiendoTabla -> {
                                            if (usarTablaPorDefectoLive.value != true) {
                                                guardarTablaCalibracionEnMemoria(acumuladorTabla.toString())
                                                recibiendoTabla = false
                                            }
                                        }

                                        // Durante recepción de tabla acumulamos líneas
                                        recibiendoTabla -> {
                                            // Añade cada línea como está
                                            acumuladorTabla.appendLine(textoRecibido)
                                        }

                                        else -> {
                                            debugMessageLiveData.postValue(textoRecibido)
                                        }
                                    }
                                }

                            }
                        } else {
                            // En modo binario: acumulamos hasta 400 bytes (200 samples * 2 bytes)
                            val bytesFaltantes = 400 - fullBuffer.size
                            val bytesDisponibles = bytesRead - offset
                            val bytesParaCopiar = minOf(bytesFaltantes, bytesDisponibles)
                            fullBuffer.addAll(buffer.copyOfRange(offset, offset + bytesParaCopiar).toList())
                            offset += bytesParaCopiar

                            if (fullBuffer.size >= 400) {
                                // Procesamos y publicamos canal y RMS
                                val values = parseShorts(fullBuffer)
                                when (currentHeader) {
                                    "#1" -> {
                                        canal1LiveData.postValue(values)
                                        val rms = calcularRmsInterno(values)
                                        rmsCanal1.postValue(rms)
                                    }
                                    "#2" -> {
                                        canal2LiveData.postValue(values)
                                        val rms = calcularRmsInterno(values)
                                        rmsCanal2.postValue(rms)
                                    }
                                    "#3" -> {
                                        canal3LiveData.postValue(values)
                                        val rms = calcularRmsInterno(values)
                                        rmsCanal3.postValue(rms)
                                    }
                                    "#4" -> {
                                        canal4LiveData.postValue(values)
                                        val rms = calcularRmsInterno(values)
                                        rmsCanal4.postValue(rms)
                                    }
                                    "#5" -> {
                                        canal5LiveData.postValue(values)
                                        val rms = calcularRmsInterno(values)
                                        rmsCanal5.postValue(rms)

                                    }
                                }
                                currentHeader = null
                                fullBuffer.clear()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error recibiendo datos: ${e.message}")
            }
        }
    }

    /**
     * Convierte lista de bytes little-endian a lista de Ints.
     */
    private fun parseShorts(bytes: List<Byte>): List<Int> {
        val result = mutableListOf<Int>()
        for (i in bytes.indices step 2) {
            val shortVal = ByteBuffer.wrap(byteArrayOf(bytes[i], bytes[i + 1]))
                .order(ByteOrder.LITTLE_ENDIAN)
                .short
                .toInt()
            result.add(shortVal)
        }
        return result
    }

    /**
     * Calcula el RMS interno utilizando utilidades de CalcularUtils.
     */
    private fun calcularRmsInterno(datos: List<Int>): Float {
        val datosInterpolados = com.example.Vacio.CalcularUtils.convertirBufferADC(datos)
        val (rms, _) = com.example.Vacio.CalcularUtils.calcularRmsYAmplitud(datosInterpolados, 1.0f)
        return rms.toFloat()
    }

    /**
     * Guarda los factores de escala FACTORES:A,B,C,IA,IB en SharedPreferences.
     */
    private fun guardarFactoresEnPreferencias(context: Context, mensaje: String) {
        val valores = mensaje.removePrefix("FACTORES:").split(",")
        if (valores.size != 5) {
            Log.e("BluetoothService", "Mensaje FACTORES no tiene 5 valores: $mensaje")
            return
        }

        try {
            val prefs = ConfiguracionPrefs(context)
            prefs.esp32FactorTensionA = valores[0].toFloat()
            prefs.esp32FactorTensionB = valores[1].toFloat()
            prefs.esp32FactorTensionC = valores[2].toFloat()
            prefs.esp32FactorCorrienteA = valores[3].toFloat()
            prefs.esp32FactorCorrienteB = valores[4].toFloat()
            Log.d("BluetoothService", "Factores ESP32 guardados en preferencias: $valores")
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error al guardar factores ESP32: ${e.message}")
        }
    }

    /**
     * Parsea y actualiza la tabla de calibración en memoria.
     */
    private fun guardarTablaCalibracionEnMemoria(tablaStr: String) {
        val nuevaTabla = tablaStr.lines().mapNotNull { linea ->
            val partes = linea.split(",")
            if (partes.size != 2) {
                Log.w("TablaCalibracion", "Par inválido: '$linea'")
                return@mapNotNull null
            }
            val volt = partes[0].toDoubleOrNull()
            val adc = partes[1].toIntOrNull()
            if (adc != null && volt != null) {
                adc to volt
            } else {
                Log.w("TablaCalibracion", "Valores inválidos: volt='$volt', adc='$adc'")
                null
            }
        }

        if (nuevaTabla.isNotEmpty()) {
            com.example.Vacio.CalcularUtils.actualizarTablaCalibracion(nuevaTabla)
            ultimaTablaRecibida = nuevaTabla // ✅ Guardamos la última
            Log.d("TablaCalibracion", nuevaTabla.joinToString("\n"))
        } else {
            Log.e("TablaCalibracion", "No se pudo generar una tabla válida.")
        }
    }



    // Cierra el socket Bluetooth
    private fun closeSocket() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.d("BluetoothService", "Socket Bluetooth cerrado correctamente.")
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al cerrar el socket: ${e.message}")
        }
    }

    // Método público para desconectar el socket
    fun disconnectsocket() {
        closeSocket()
        connectionStatusLiveData.postValue(false)
    }

    // Al destruir el servicio, cerramos el socket
    override fun onDestroy() {
        super.onDestroy()
        closeSocket()
    }
}
