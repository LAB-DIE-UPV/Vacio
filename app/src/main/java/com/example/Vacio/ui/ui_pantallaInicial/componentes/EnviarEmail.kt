package com.example.Vacio.ui.ui_pantallaInicial.componentes

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File


/**
 * Envía múltiples archivos PDF por correo electrónico usando un Intent ACTION_SEND_MULTIPLE.
 * Utiliza FileProvider para obtener URIs legibles para otras apps.
 *
 * @param context Context de la Activity para lanzar el Intent y mostrar Toasts.
 * @param pdfFiles Lista de archivos PDF a enviar.
 */
fun enviarMultiplesPdfsPorEmail(context: Context, pdfFiles: List<File>) {
    // Si no hay archivos, notificar al usuario y salir
    if (pdfFiles.isEmpty()) {
        Toast.makeText(context, "No hay archivos para enviar", Toast.LENGTH_SHORT).show()
        return
    }

    // Convierte cada File en un URI seguro usando FileProvider
    val uris = pdfFiles.map { file ->
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // Crea un Intent para enviar múltiples archivos
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        // MIME type de PDF
        type = "application/pdf"
        // Lista de URIs de los archivos adjuntos
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        // Asunto del correo
        putExtra(Intent.EXTRA_SUBJECT, "Informes Circuitos Magnéticos")
        // Texto del correo
        putExtra(Intent.EXTRA_TEXT, "Adjunto(s) informes de medición de circuitos magnéticos.")
        // Permite que las apps receptoras lean los URIs
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Intenta lanzar un chooser para que el usuario seleccione la app de correo
    try {
        context.startActivity(
            Intent.createChooser(intent, "Enviar informes PDF con...")
        )
    } catch (e: Exception) {
        // Si falla, muestra un Toast con el error
        Toast.makeText(
            context,
            "No se pudo enviar: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}


