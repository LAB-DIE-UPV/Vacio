package com.example.Vacio.CalcularUtils

import kotlin.math.abs
import kotlin.math.sqrt

// ---------------------------------------------
// Tabla de calibración
// Mapea valores crudos del ADC a valores físicos (tensión/corriente)
// ---------------------------------------------

// ⚠️ Tabla por defecto (constante, no se debe modificar)
val tablaPorDefecto = listOf(
    0 to 0.0, 0 to 0.1, 24 to 0.15, 82 to 0.2, 160 to 0.25,
    226 to 0.3, 288 to 0.35, 352 to 0.4, 406 to 0.45, 474 to 0.5,
    528 to 0.55, 586 to 0.6, 653 to 0.65, 716 to 0.7, 768 to 0.75,
    832 to 0.8, 896 to 0.85, 955 to 0.9, 1019 to 0.95, 1078 to 1.0,
    1138 to 1.05, 1200 to 1.1, 1264 to 1.15, 1324 to 1.2, 1382 to 1.25,
    1447 to 1.3, 1504 to 1.35, 1574 to 1.4, 1634 to 1.45, 1696 to 1.5,
    1751 to 1.55, 1813 to 1.6, 1871 to 1.65, 1920 to 1.7, 1982 to 1.75,
    2047 to 1.8, 2125 to 1.85, 2179 to 1.9, 2242 to 1.95, 2302 to 2.0,
    2355 to 2.05, 2422 to 2.1, 2483 to 2.15, 2543 to 2.2, 2613 to 2.25,
    2671 to 2.3, 2735 to 2.35, 2791 to 2.4, 2855 to 2.45, 2923 to 2.5,
    3000 to 2.55, 3055 to 2.6, 3145 to 2.65, 3227 to 2.7, 3340 to 2.75,
    3443 to 2.8, 3538 to 2.85, 3635 to 2.9, 3731 to 2.95, 3839 to 3.0,
    3959 to 3.05, 4095 to 3.1, 4095 to 3.15
)

// Tabla activa que se usará en los cálculos, se puede modificar en tiempo de ejecución
var tablaCalibracion: List<Pair<Int, Double>> = tablaPorDefecto.toList()

fun actualizarTablaCalibracion(nuevaTabla: List<Pair<Int, Double>>) {
    tablaCalibracion = nuevaTabla.sortedBy { it.first }
}


// ---------------------------------------------
// Interpolación lineal en la tabla de calibración
// Devuelve el valor físico correspondiente al adcValue
// ---------------------------------------------
fun interpolarCalibracion(adcValue: Int): Double {
    // Si el valor está fuera del rango, devuelve el extremo correspondiente
    if (adcValue <= tablaCalibracion.first().first) return tablaCalibracion.first().second
    if (adcValue >= tablaCalibracion.last().first) return tablaCalibracion.last().second

    // Recorre pares consecutivos para encontrar el intervalo que contiene adcValue
    for (i in 0 until tablaCalibracion.size - 1) {
        val (x0, y0) = tablaCalibracion[i]
        val (x1, y1) = tablaCalibracion[i + 1]
        if (adcValue in x0..x1) {
            // Fórmula de interpolación lineal
            return y0 + (adcValue - x0) * (y1 - y0) / (x1 - x0)
        }
    }
    // Valor por defecto (no debería llegar aquí)
    return 0.0
}

// ---------------------------------------------
// Convierte una lista de valores ADC crudos a valores calibrados
// ---------------------------------------------
fun convertirBufferADC(datos: List<Int>): List<Double> {
    return datos.map { interpolarCalibracion(it) }
}

// ---------------------------------------------
// Calcula RMS y amplitud máxima de una señal calibrada
// Parámetros:
// - datos: lista de valores calibrados
// - factor: factor de escala (por ejemplo, factor de conversión física)
// Devuelve Pair(rms, amplitud)
// ---------------------------------------------
fun calcularRmsYAmplitud(datos: List<Double>, factor: Float): Pair<Double, Double> {
    // Elimina offset DC usando la media
    val offset = datos.average()
    // Aplica factor de escala y resta offset
    val señalConvertida = datos.map { (it - offset) * factor }
    // Cálculo RMS: raíz cuadrada de la media de cuadrados
    val rms = sqrt(señalConvertida.map { it * it }.average())
    // Amplitud: máximo valor absoluto
    val amplitud = señalConvertida.maxOfOrNull { abs(it) } ?: 0.0
    return Pair(rms, amplitud)
}

// ---------------------------------------------
// Calcula el voltaje RMS a partir de datos ADC crudos
// Devuelve el valor formateado como String con 1 decimal
// ---------------------------------------------
fun calcularVoltajeRms(datos: List<Int>, factor: Float): String {
    if (datos.isEmpty()) return "--"  // Indica falta de datos
    val datosInterpolados = convertirBufferADC(datos)
    val (rms, _) = calcularRmsYAmplitud(datosInterpolados, factor)
    return "%.1f".format(rms)
}

// ---------------------------------------------
// Calcula la corriente RMS a partir de datos ADC crudos
// Devuelve el valor formateado como String con 2 decimales
// ---------------------------------------------
fun calcularCorrienteRms(datos: List<Int>, factor: Float = 1.0f): String {
    if (datos.isEmpty()) return "--"
    val datosInterpolados = convertirBufferADC(datos)
    val (rms, _) = calcularRmsYAmplitud(datosInterpolados, factor)
    return "%.2f".format(rms)
}

// ---------------------------------------------
// Calcula potencia aparente, activa y factor de potencia
// a partir de listas de corriente y voltaje ADC crudos
// Devuelve Triple(potAparente, potActiva, factorPot)
// ---------------------------------------------
fun calcularPotenciasYFactor(
    canalCorriente: List<Int>,
    canalVoltaje: List<Int>,
    fCorriente: Float = 1.0f,
    fVoltaje: Float = 179.6f
): Triple<String, String, String> {
    if (canalCorriente.isEmpty() || canalVoltaje.isEmpty())
        return Triple("--", "--", "--")

    // Interpolar y eliminar offset
    val corrienteInterpolada = convertirBufferADC(canalCorriente)
    val voltajeInterpolada = convertirBufferADC(canalVoltaje)

    val corrienteSinOffset = corrienteInterpolada.map { it - corrienteInterpolada.average() }
    val voltajeSinOffset = voltajeInterpolada.map { it - voltajeInterpolada.average() }

    // Escalar según factores físicos
    val corrienteEscalada = corrienteSinOffset.map { it * fCorriente }
    val voltajeEscalada = voltajeSinOffset.map { it * fVoltaje }

    // Potencia activa: promedio de producto V * I
    val potenciaActiva = corrienteEscalada.zip(voltajeEscalada)
        .map { abs(it.first * it.second) }
        .average()

    // Potencia aparente: Irms * Vrms
    val (irms, _) = calcularRmsYAmplitud(corrienteInterpolada, fCorriente)
    val (vrms, _) = calcularRmsYAmplitud(voltajeInterpolada, fVoltaje)
    val potenciaAparente = irms * vrms

    // Evita división por cero
    if (potenciaAparente == 0.0)
        return Triple(
            "%.1f".format(potenciaAparente),
            "%.1f".format(potenciaActiva),
            "--"
        )

    // Factor de potencia = activa/aparente
    val fp = potenciaActiva / potenciaAparente

    // Formatea resultados
    return Triple(
        "%.1f".format(potenciaAparente),
        "%.1f".format(potenciaActiva),
        "%.2f".format(fp)
    )
}

// ---------------------------------------------
// Normaliza una lista de enteros entre -1 y +1
// Útil para escalado de gráficos
// ---------------------------------------------
fun normalizarLista(valores: List<Int>): List<Float> {
    if (valores.isEmpty()) return emptyList()

    val max = valores.maxOrNull()?.toFloat() ?: 1f
    val min = valores.minOrNull()?.toFloat() ?: 0f

    return if (max != min) {
        valores.map { v -> -1f + 2f * ((v - min) / (max - min)) }
    } else {
        // Si todos los valores son iguales, devuelve ceros
        List(valores.size) { 0f }
    }
}