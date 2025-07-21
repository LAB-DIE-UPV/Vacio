package com.example.Vacio.ui.ui_pantallaCalibracion

import android.graphics.Paint

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Muestra un gráfico de líneas con datos crudos del ADC (valores de 0 a 4095).
 * Utiliza MPAndroidChart embebido en Jetpack Compose mediante AndroidView.
 *
 * @param title   Título mostrado encima del gráfico.
 * @param series  Lista de pares (etiqueta, lista de muestras ADC) para cada canal.
 * @param modifier Modificador de Compose para tamaño y estilo del componente.
 */
@Composable
fun RawCalibrationChart(
    title: String,
    series: List<Pair<String, List<Int>>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier, // Modificador externo para posicionamiento y tamaño
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título encima del gráfico
        Text(text = title, style = MaterialTheme.typography.bodyMedium)

        // Fila que contiene el gráfico y la etiqueta del eje Y
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Etiqueta girada para el eje Y (izquierda del gráfico)
            VerticalAxisLabel(text = "ADC (0–4095)")

            // Contenedor del gráfico en sí
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // 👈 aquí también opcionalmente
                contentAlignment = Alignment.Center
            ) {
                // Vista nativa de Android embebida: LineChart de MPAndroidChart
                AndroidView(
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            // Configuración general del gráfico
                            description.isEnabled = false
                            legend.isEnabled = true
                            axisRight.isEnabled = false  // Solo usamos eje izquierdo

                            // Configuración del eje X (tiempo)
                            xAxis.axisMinimum = 0f
                            xAxis.axisMaximum = 20f        // Máx. 20 ms
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.setDrawGridLines(false)
                            xAxis.valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return "${value.toInt()}" // Etiquetas enteras
                                }
                            }

                            // Configuración del eje Y (valor ADC crudo)
                            axisLeft.axisMinimum = 0f
                            axisLeft.axisMaximum = 4095f
                            axisLeft.setDrawGridLines(false)

                            // Interacción desactivada
                            setTouchEnabled(false)
                            setScaleEnabled(false)
                            setPinchZoom(false)
                        }
                    },

                    // Actualiza el contenido del gráfico cuando cambia la serie
                    update = { chart ->
                        // Calcula paso de tiempo entre muestras
                        val n = series.firstOrNull()?.second?.size ?: 1
                        val tStep = 20f / n  // Si tienes 200 muestras, será 0.1 ms

                        // Crea un LineDataSet para cada canal
                        val dataSets = series.mapIndexed { idx, (label, values) ->
                            val entries = values.mapIndexed { i, v ->
                                Entry(i * tStep, v.toFloat()) // X: tiempo, Y: valor ADC
                            }

                            LineDataSet(entries, label).apply {
                                setDrawCircles(false)           // No dibuja puntos
                                lineWidth = 2f
                                color = ColorTemplate.COLORFUL_COLORS[idx % ColorTemplate.COLORFUL_COLORS.size]
                            }
                        }

                        // Asigna los datos al gráfico y lo refresca
                        chart.data = LineData(dataSets)
                        chart.invalidate()
                    },

                    modifier = Modifier.matchParentSize() // Ocupa todo el Box
                )
            }
        }

        // Etiqueta del eje X debajo del gráfico
        Text(text = "Tiempo (ms)", style = MaterialTheme.typography.labelSmall)
    }
}


/**
 * Dibuja texto vertical girado 90° para el eje Y.
 * @param text Texto a mostrar.
 * @param modifier Modificador de layout.
 * @param textColor Color del texto.
 * @param textSizeSp Tamaño de texto en SP.
 */

@Composable
fun VerticalAxisLabel(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    textSizeSp: Float = 12f // puedes ajustar este valor según tu gusto
) {
    Box(
        modifier = modifier
            .width(10.dp) // Ancho ajustable según necesidades
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Configura Paint nativo para dibujar el texto
            val paint = Paint().apply {
                color = textColor.toArgb()
                textSize = textSizeSp.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val xPos = size.width / 2
            val yPos = size.height / 2 - (paint.descent() + paint.ascent()) / 2

            // Rota y dibuja texto
            rotate(degrees = -90f, pivot = Offset(xPos, yPos)) {
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    xPos,
                    yPos,
                    paint
                )
            }
        }
    }
}







