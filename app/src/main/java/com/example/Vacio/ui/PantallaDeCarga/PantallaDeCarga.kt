package com.example.Vacio.ui.PantallaDeCarga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * SimpleSplash: pantalla de carga básica con logo, mensaje y spinner.
 * @param message Mensaje a mostrar bajo el título.
 * @param totalDurationMs Duración mínima en milisegundos antes de llamar onTimeout.
 * @param onTimeout Callback que se invoca tras expirar el timeout.
 */
@Composable
fun SimpleSplash(
    message: String = "Iniciando servicio Bluetooth…",
    totalDurationMs: Long = 2000L,
    onTimeout: () -> Unit
) {
    // Lanzamos un efecto que espera totalDurationMs y luego ejecuta onTimeout
    LaunchedEffect(Unit) {
        delay(totalDurationMs)   // Pausa sin bloquear el hilo UI
        onTimeout()              // Notifica que el splash ha terminado
    }

    // Estructura de la UI centrada en pantalla completa
    Box(
        modifier = Modifier
            .fillMaxSize()                                          // Ocupa toda la pantalla
            .background(MaterialTheme.colorScheme.background),      // Fondo con color del tema
        contentAlignment = Alignment.Center                        // Centrar contenido interior
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,    // Centrar horizontalmente
            verticalArrangement = Arrangement.Center               // Centrar verticalmente
        ) {
            /*
            // Imagen del logo: ocupa el 60% del ancho, manteniendo proporción cuadrada
            Image(
                painter = painterResource(id = R.drawable.transformador_electrico),
                contentDescription = "Logo de Osci",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .padding(bottom = 24.dp)                        // Espacio bajo la imagen
            )
             */

            Spacer(Modifier.height(24.dp))                          // Separador vertical

            // Título principal de la splash screen
            Text(
                text = "Proyecto: Vacio",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))                          // Separador

            // Mensaje informativo dinámico
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))                          // Separador

            // Indicador de progreso circular indeterminado
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),                    // Tamaño fijo
                strokeWidth = 4.dp                                   // Grosor del trazo
            )
        }
    }
}
