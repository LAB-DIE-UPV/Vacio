package com.example.Vacio.ui.ui_pantallaInicial.componentes

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Diálogo que muestra los informes PDF generados en las últimas 3 horas (máx. 7 archivos).
 * Permite ver, borrar o enviar todos los PDFs por email.
 *
 * @param context Contexto de la Activity para acceder a archivos y funciones de apertura/envío.
 * @param onDismiss Callback para cerrar el diálogo.
 */
@Composable
fun DialogoSeleccionPdf(
    context: Context,
    onDismiss: () -> Unit
) {
    // Directorio donde se guardan los documentos PDF de la app
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    // Lista reactiva de archivos PDF filtrados por fecha (últimas 3h) y ordenada por fecha descendente
    val pdfFiles = remember {
        val tresHorasEnMillis = 3 * 60 * 60 * 1000      // 3 horas en milisegundos
        val ahora = System.currentTimeMillis()           // Marca de tiempo actual

        mutableStateListOf<File>().apply {
            // Filtrar archivos .pdf modificados en las últimas 3 horas
            addAll(
                dir?.listFiles { file ->
                    file.name.endsWith(".pdf") &&
                            (ahora - file.lastModified() <= tresHorasEnMillis)
                }
                    ?.sortedByDescending { it.lastModified() }  // ordenar del más nuevo al más antiguo
                    ?.take(7)                                     // máximo 7 elementos
                    ?: emptyList()
            )
        }
    }

    // Estado para almacenar el archivo seleccionado para borrar
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    // Diálogo principal de selección de PDFs
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informes PDF (Máximo 7)") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                if (pdfFiles.isEmpty()) {
                    // Mensaje cuando no hay archivos disponibles
                    Text("No hay informes disponibles.")
                } else {
                    // Mostrar cada archivo en una fila con acciones Ver y Borrar
                    pdfFiles.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Nombre del archivo
                            Text(
                                text = file.name,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Botón para abrir el PDF
                            Button(onClick = { abrirPdf(context, file) }) {
                                Text("Ver")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Botón para marcar el archivo para borrado
                            Button(
                                onClick = { fileToDelete = file },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Borrar")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Botón para enviar todos los PDFs por email si la lista no está vacía
            if (pdfFiles.isNotEmpty()) {
                TextButton(
                    onClick = {
                        enviarMultiplesPdfsPorEmail(context, pdfFiles)
                        onDismiss()
                    }
                ) {
                    Text("Enviar Todos")
                }
            }
        },
        dismissButton = {
            // Botón para cerrar el diálogo sin acción
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    // Diálogo de confirmación para borrar un archivo PDF seleccionado
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que deseas borrar el archivo ${file.name}?" ) },
            confirmButton = {
                TextButton(
                    onClick = {
                        file.delete()          // Borra físicamente el archivo
                        pdfFiles.remove(file)  // Actualiza la lista reactiva
                        fileToDelete = null    // Cierra este diálogo de confirmación
                    }
                ) {
                    Text("Sí, borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
