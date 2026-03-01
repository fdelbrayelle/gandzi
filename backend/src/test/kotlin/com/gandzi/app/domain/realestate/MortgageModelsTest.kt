package com.gandzi.app.domain.realestate

import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MortgageModelsTest {
    @Test
    fun `should compute owned value from co ownership share`() {
        val residence = PrimaryResidence("r1", "Main Home", Money.of("400000.00"), 50.0)
        assertEquals("200000.00", residence.ownedValue().amount.toPlainString())
    }

    @Test
    fun `should compute decreasing outstanding principal`() {
        val mortgage = FixedRateMortgage(Money.of("200000.00"), annualRatePercent = 3.0, durationMonths = 240)

        val start = mortgage.outstandingPrincipal(0).amount
        val month12 = mortgage.outstandingPrincipal(12).amount

        assertTrue(month12 < start)
    }
}
