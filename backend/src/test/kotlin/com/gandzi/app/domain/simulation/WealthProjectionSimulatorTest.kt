package com.gandzi.app.domain.simulation

import com.gandzi.app.domain.shared.Money
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WealthProjectionSimulatorTest {
    @Test
    fun `deterministic projection should produce requested horizon`() {
        val simulator = WealthProjectionSimulator()
        val result = simulator.deterministic(
            WealthProjectionInput(
                initialWealth = Money.of("100000.00"),
                annualIncome = Money.of("40000.00"),
                annualExpenses = Money.of("25000.00"),
                years = 30,
                expectedAnnualReturnPercent = 5.0,
                inflationPercent = 2.0,
                incomeGrowthPercent = 1.0,
                expensesGrowthPercent = 1.5,
            ),
        )

        assertEquals(30, result.yearlyValues.size)
    }

    @Test
    fun `monte carlo should return ordered percentiles`() {
        val simulator = WealthProjectionSimulator(Random(123))
        val result = simulator.monteCarlo(
            WealthProjectionInput(
                initialWealth = Money.of("100000.00"),
                annualIncome = Money.of("50000.00"),
                annualExpenses = Money.of("30000.00"),
                years = 20,
                expectedAnnualReturnPercent = 6.0,
                inflationPercent = 2.0,
                incomeGrowthPercent = 1.0,
                expensesGrowthPercent = 1.0,
            ),
            trials = 200,
        )

        assertTrue(result.p90.amount >= result.p50.amount)
        assertTrue(result.p50.amount >= result.p10.amount)
    }
}
