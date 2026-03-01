package com.gandzi.app.domain.portability

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataPortabilityTest {
    @Test
    fun `should generate timestamped export filename`() {
        val name = DataExportNaming.fileName(
            ExportRequest(
                format = ExportFormat.JSON,
                generatedAt = Instant.parse("2026-03-01T12:34:56.789Z"),
            ),
        )

        assertEquals("gandzi_export_20260301_123456_789.json", name)
    }

    @Test
    fun `should merge or replace based on import mode`() {
        val service = ImportService()
        val existing = listOf("a", "b")
        val incoming = listOf("b", "c")

        val merged = service.import(ImportMode.MERGE, existing, incoming)
        val replaced = service.import(ImportMode.REPLACE, existing, incoming)

        assertTrue(merged.containsAll(listOf("a", "b", "c")))
        assertEquals(incoming, replaced)
    }
}
