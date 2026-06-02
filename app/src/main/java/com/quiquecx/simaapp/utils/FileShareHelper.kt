package com.quiquecx.simaapp.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.quiquecx.simaapp.domain.entity.ReportFormat
import java.io.File
import javax.inject.Inject

class FileShareHelper @Inject constructor() {

    fun share(file: File, format: ReportFormat, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = when (format) {
                ReportFormat.PDF -> "text/html"
                ReportFormat.EXCEL -> "text/csv"
                ReportFormat.CSV -> "text/csv"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Compartir reporte")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al compartir: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}