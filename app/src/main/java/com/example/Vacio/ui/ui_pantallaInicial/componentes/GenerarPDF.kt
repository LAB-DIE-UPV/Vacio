package com.example.Vacio.ui.ui_pantallaInicial.componentes

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import android.view.View

/**
 * Genera y guarda un PDF con datos de medición y gráficas, además de
 * una captura de pantalla opcional.
 *
 * @param context Context de la Activity para acceder a archivos y mostrar Toast.
 * @param nombreBase Nombre base que el usuario proporciona para el archivo.
 * @param screenshot Bitmap de la captura de pantalla que se incluirá en el PDF (opcional).
 * @return File que apunta al PDF generado o null en caso de error.
 */

fun generarInformePdfConNombre(
    context: Context,
    nombreBase: String,
    screenshot: Bitmap? = null
): File? {
    // Crea el documento PDF vacío
    val document = PdfDocument()

    // Configura la primera página con tamaño A4 en puntos (595x842)
    val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page1 = document.startPage(pageInfo1)
    val c1 = page1.canvas
    val paint = Paint().apply {
        isAntiAlias = true
    }

    // Paint para dibujar texto y gráficos
    // Encabezado
    paint.textSize = 20f
    paint.isFakeBoldText = true
    c1.drawText("Informe de medición - ${fechaActual()}", 40f, 50f, paint)

    // 2) Si se recibe una captura, la escala y dibuja centrada
    if (screenshot != null) {
        // Dimensiones de la página y márgenes
        val pageW = 595f
        val pageH = 842f
        val mH = 40f
        val topOff = 80f
        val botOff = 40f

        // Calcula espacio disponible y escala proporcionalmente
        val availW = pageW - 2*mH
        val availH = pageH - topOff - botOff
        val oW = screenshot.width.toFloat()
        val oH = screenshot.height.toFloat()
        val ratio = minOf(availW/oW, availH/oH)
        val sw = (oW*ratio).toInt()
        val sh = (oH*ratio).toInt()
        val bmp2 = screenshot.scale(sw, sh)
        // Calcula offsets para centrar la imagen
        val offX = mH + (availW - sw)/2
        val offY = topOff + (availH - sh)/2
        // Dibuja la imagen en el canvas
        c1.drawBitmap(bmp2, offX, offY, null)
    }

    // Finaliza la primera página
    document.finishPage(page1)

    // 3) Prepara la ruta y el nombre seguro del archivo
    // Guardar en Documents/Informe_...pdf
    val prefijo = "LAB_DIE_MAGNETICOS"
    // Directorio Documents dentro del storage de la app
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.apply { if (!exists()) mkdirs() }
    // Sufijo con fecha y hora para evitar colisiones
    val sufijo = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
    // Reemplaza caracteres no permitidos en el nombre del usuario
    val safeName = nombreBase.trim().replace("""[^\w\s-]""".toRegex(), "_")
    val out = File(dir, "${prefijo}_${safeName}_$sufijo.pdf")


    return try {
        // Escribe el PDF en el archivo y cierra el documento
        FileOutputStream(out).use { document.writeTo(it) }
        document.close()
        // Notifica éxito al usuario
        Toast.makeText(context, "PDF generado:\n${out.absolutePath}", Toast.LENGTH_SHORT).show()
        out
    } catch(e:Exception){
        // Manejo de errores: imprime stacktrace y muestra mensaje
        e.printStackTrace()
        Toast.makeText(context, "Error generando PDF: ${e.message}", Toast.LENGTH_LONG).show()
        null
    }
}

/**
 * Obtiene la fecha y hora actual formateada como dd/MM/yyyy HH:mm.
 */
private fun fechaActual(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

/**
 * Captura la vista completa a alta resolución (300 dpi) y elimina
 * barras superiores e inferiores si se indican.
 *
 * @param view Vista raíz que se va a capturar.
 * @param context Contexto para obtener densidad de pantalla.
 * @param topBarHeightPx Alto en px de la barra superior a recortar.
 * @param bottomBarHeightPx Alto en px de la barra inferior a recortar.
 * @return Bitmap de la captura final.
 */

fun capturarVistaAltaCalidadSinBarras(
    view: View,
    context: Context,
    topBarHeightPx: Int = 0,
    bottomBarHeightPx: Int = 0
): Bitmap {
    val dpi     = context.resources.displayMetrics.densityDpi.toFloat()
    val targetDpi = 300f
    val scale   = targetDpi / dpi
    val w       = (view.width * scale).toInt()
    val h       = (view.height * scale).toInt()
    val bmpFull = createBitmap(w, h)
    val canvas  = android.graphics.Canvas(bmpFull)
    canvas.scale(scale, scale)
    view.draw(canvas)
    val top    = (topBarHeightPx * scale).toInt()
    val bottom = (bottomBarHeightPx * scale).toInt()
    val finalH = h - top - bottom
    return Bitmap.createBitmap(bmpFull, 0, top, w, maxOf(1, finalH))
}

/**
 * Abre un archivo PDF usando un Intent, manejando permisos via FileProvider.
 * Si no hay app para abrirlo, muestra un Toast.
 */
fun abrirPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No hay app para abrir PDF instalada", Toast.LENGTH_SHORT).show()
    }
}

/*
/**
 * Combina dos gráficas:
 *  1) Últimas 200 muestras (normalizadas) — corriente y voltaje
 *  2) Hasta 600 muestras completas — curva de histéresis
 */
fun generarGraficaCombinadaBitmap(
    context: Context,
    canal1: List<Int>,
    canal2: List<Int>
): Bitmap? {
    if (canal1.isEmpty() || canal2.isEmpty()) return null

    // --- Dimensiones de las gráficas ---
    val chartWidth = 600
    val chartHeight = 400

    // --- Márgenes y desplazamientos generales ---
    val marginLegendRight = 300f
    val marginExtraLeft = 150f

    // --- Espacios entre elementos ---
    val marginTopTitle = 100f
    val marginAfterTitle = 70f
    val marginAfterGraph = 70f
    val gapBetweenGraphs = 270f
    val additionalYOffsetSecondGraph = -20f // ajuste fino vertical segunda gráfica

    // --- Texto ---
    val textSizeTitle = 54f
    val textSizeAxis = 42f
    val textSizeAxisY = 48f
    val textSizeLegend = 40f

    // --- Datos normalizados ---
    val c200 = canal1.takeLast(200)
    val v200 = canal2.takeLast(200)
    val nC = normalizarLista(c200)
    val nV = normalizarLista(v200)
    val tiempoMsPorMuestra = 20f / 200f

    // Dataset corriente
    val dsC = LineDataSet(nC.mapIndexed { i, y -> Entry(i * tiempoMsPorMuestra, y) }, "Corriente (A)").apply {
        color = Color.BLUE; lineWidth = 3f; setDrawCircles(false); setDrawValues(false)
    }

    // Dataset voltaje
    val dsV = LineDataSet(nV.mapIndexed { i, y -> Entry(i * tiempoMsPorMuestra, y) }, "Voltaje (V)").apply {
        color = Color.RED; lineWidth = 3f; setDrawCircles(false); setDrawValues(false)
    }

    // Gráfico 1
    val chart1 = LineChart(context).apply {
        data = LineData(dsC, dsV)
        description.isEnabled = false
        legend.isEnabled = false
        axisRight.isEnabled = false
        axisLeft.apply {
            textSize = textSizeAxis; axisMinimum = -1.2f; axisMaximum = 1.2f
            setDrawGridLines(false); setDrawAxisLine(true)
            axisLineColor = Color.BLACK; axisLineWidth = 2f
        }
        xAxis.apply {
            textSize = textSizeAxis; position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false); setDrawAxisLine(true)
            axisLineColor = Color.BLACK; axisLineWidth = 2f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = value.toInt().toString()
            }
            axisMinimum = 0f; axisMaximum = 21f
        }
        setExtraOffsets(10f, 10f, 10f, 10f)
        layout(0, 0, chartWidth, chartHeight)
        measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        )
    }
    chart1.invalidate()
    val bmp1 = createBitmap(chartWidth, chartHeight).apply {
        android.graphics.Canvas(this).also { chart1.draw(it) }
    }

    // Dataset histéresis
    val factorCorriente = ConfiguracionPrefs(context).factorCorrienteA
    val factorVoltaje = ConfiguracionPrefs(context).factorTensionA

    val corrienteReal = canal1.map { it * factorCorriente / 4095f }
    val voltajeReal = canal2.map { it * factorVoltaje / 4095f }

    val datosHB = calcularCurvaHisteresisConDesfase90(corrienteReal, voltajeReal)

    val dsH = LineDataSet(datosHB.map { (h, b) -> Entry(h, b) }, "Histéresis").apply {
        color = Color.MAGENTA; lineWidth = 3f; setDrawCircles(false); setDrawValues(false)
    }

    // Gráfico 2
    val chart2 = LineChart(context).apply {
        data = LineData(dsH)
        description.isEnabled = false
        legend.isEnabled = false
        axisRight.isEnabled = false
        axisLeft.apply {
            textSize = textSizeAxis; axisMinimum = -550f; axisMaximum = 550f
            setDrawGridLines(false); setDrawAxisLine(true)
            axisLineColor = Color.BLACK; axisLineWidth = 2f
        }
        xAxis.apply {
            textSize = textSizeAxis; position = XAxis.XAxisPosition.BOTTOM
            axisMinimum = -120f; axisMaximum = 120f
            setDrawGridLines(false); setDrawAxisLine(true)
            axisLineColor = Color.BLACK; axisLineWidth = 2f
        }
        setExtraOffsets(10f, 10f, 35f, 15f)
        layout(0, 0, chartWidth, chartHeight)
        measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        )
    }
    chart2.invalidate()
    val bmp2 = createBitmap(chartWidth, chartHeight).apply {
        android.graphics.Canvas(this).also { chart2.draw(it) }
    }


    // --- Posiciones calculadas ---
    val titleBounds = android.graphics.Rect().also {
        Paint().apply { textSize = textSizeTitle; isFakeBoldText = true }.getTextBounds(
            "Corriente y Voltaje (normalizados)", 0, "Corriente y Voltaje (normalizados)".length, it
        )
    }
    val titleHeight = titleBounds.height().toFloat()

    val posGraph1Top = marginTopTitle + titleHeight + marginAfterTitle
    val posLabel1Y = posGraph1Top + chartHeight + marginAfterGraph
    val posGraph2Top = posLabel1Y + textSizeAxis + gapBetweenGraphs
    val posLabel2Y = posGraph2Top + chartHeight + marginAfterGraph

    val totalWidth = chartWidth + marginLegendRight + marginExtraLeft
    val totalHeight = (posLabel2Y + textSizeAxis + 150f).toInt()

    val canvasBitmap = createBitmap(totalWidth.toInt(), totalHeight)
    val canvas = android.graphics.Canvas(canvasBitmap).apply { drawColor(Color.WHITE) }

    // --- Paints ---
    val paintTitle = Paint().apply { color = Color.BLACK; textSize = textSizeTitle; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    val paintAxis = Paint().apply { color = Color.BLACK; textSize = textSizeAxis; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    val paintAxisY = Paint().apply { color = Color.BLACK; textSize = textSizeAxisY; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    val paintLegend = Paint().apply { color = Color.BLACK; textSize = textSizeLegend; isFakeBoldText = true; textAlign = Paint.Align.LEFT }

    // --- Dibujo ---
    canvas.drawText("Corriente y Voltaje (normalizados)", marginExtraLeft + (chartWidth / 2f), marginTopTitle + titleHeight, paintTitle)

    val legendX = marginExtraLeft + chartWidth + 10f
    canvas.drawText("🔵 Corriente (A)", legendX, posGraph1Top + 100f, paintLegend)
    canvas.drawText("🔴 Voltaje (V)", legendX, posGraph1Top + 160f, paintLegend)

    canvas.drawBitmap(bmp1, marginExtraLeft, posGraph1Top, null)
    canvas.drawText("Tiempo (ms)", marginExtraLeft + chartWidth / 2f, posLabel1Y, paintAxis)

    val numWidth = paintAxis.measureText("-120")
    val translateX = numWidth - 90f
    canvas.withTranslation(marginExtraLeft + translateX, posGraph1Top + chartHeight / 2f) {
        rotate(-90f)
        drawText("Amperios - Voltaje", 0f, 0f, paintAxisY)
    }

    val titleHisteresisY = posGraph2Top - gapBetweenGraphs / 2f + 25f
    canvas.drawText("Curva de Histéresis", marginExtraLeft + chartWidth / 2f, titleHisteresisY, paintTitle)

    canvas.drawText("🟣 Histéresis", legendX, posGraph2Top + 100f, paintLegend)
    canvas.drawBitmap(bmp2, marginExtraLeft, posGraph2Top + additionalYOffsetSecondGraph, null)

    canvas.drawText("Campo magnético H", marginExtraLeft + chartWidth / 2f, posLabel2Y, paintAxis)
    canvas.drawText("(A·vuelta/m)", marginExtraLeft + chartWidth / 2f, posLabel2Y + 32f * 1.2f, paintAxis)

    canvas.withTranslation(marginExtraLeft + translateX, posGraph2Top + chartHeight / 2f) {
        rotate(-90f)
        drawText("Inducción magnética B (T)", 0f, 0f, paintAxisY)
    }

    return canvasBitmap
}
*/

/**
 * Buffer rodante de hasta 600 muestras para ambos canales.
 * Llámalo siempre que recibas un bloque NUEVO de 200 datos:
 *
 *    HistorialHisteresis.añadeMuestras(canal1, canal2)
 */
object HistorialHisteresis {
    private const val MAX = 600
    private val bufC = mutableListOf<Int>()
    private val bufV = mutableListOf<Int>()

    fun añadeMuestras(corriente: List<Int>, voltaje: List<Int>) {
        bufC += corriente
        bufV += voltaje
        if(bufC.size>MAX) bufC.subList(0, bufC.size-MAX).clear()
        if(bufV.size>MAX) bufV.subList(0, bufV.size-MAX).clear()
    }
    fun getCorriente(): List<Int> = bufC
    fun getVoltaje():   List<Int> = bufV
}
