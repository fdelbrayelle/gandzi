package com.gandzi.app.domain.wealth

import com.gandzi.app.domain.shared.Money
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class WealthSnapshotTest {
    @Test
    fun `should compute gross net and financial wealth`() {
        val snapshot = WealthSnapshot(
            takenAt = Instant.parse("2026-01-01T00:00:00Z"),
            frequency = SnapshotFrequency.MONTHLY,
            assets = listOf(
                Asset("a1", "Bitcoin", AssetCategory.CRYPTO, HoldingSupport.CRYPTO_WALLET, Money.of("10000.00")),
                Asset("a2", "Primary Residence", AssetCategory.REAL_ESTATE, HoldingSupport.OTHER, Money.of("250000.00")),
            ),
            liabilities = listOf(
                Liability("l1", "Mortgage", Money.of("100000.00")),
            ),
        )

        assertEquals("260000.00", snapshot.grossWealth().amount.toPlainString())
        assertEquals("160000.00", snapshot.netWealth().amount.toPlainString())
        assertEquals("10000.00", snapshot.financialWealth().amount.toPlainString())
    }
}
