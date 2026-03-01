package com.gandzi.app.domain.portability

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class ExportFormat {
    JSON, CSV
}

enum class ImportMode {
    MERGE, REPLACE
}

data class ExportRequest(
    val format: ExportFormat,
    val generatedAt: Instant,
)

object DataExportNaming {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC)

    fun fileName(request: ExportRequest): String {
        val ts = formatter.format(request.generatedAt)
        val ext = if (request.format == ExportFormat.JSON) "json" else "csv"
        return "gandzi_export_${ts}.${ext}"
    }
}

class ImportService {
    fun import(mode: ImportMode, existingRows: List<String>, incomingRows: List<String>): List<String> {
        return when (mode) {
            ImportMode.MERGE -> (existingRows + incomingRows).distinct()
            ImportMode.REPLACE -> incomingRows
        }
    }
}
