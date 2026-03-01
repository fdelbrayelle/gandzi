package com.gandzi.app.domain.simulation

import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertTrue

class CompoundInterestSimulatorTest {
    @Test
    fun `should grow principal with recurring contributions`() {
        val simulator = CompoundInterestSimulator()
        val result = simulator.simulate(
            CompoundInterestInput(
                principal = Money.of("10000.00"),
                years = 10,
                distribution = RateDistribution.MONTHLY,
                annualRatePercent = 6.0,
                recurringContribution = Money.of("100.00"),
            ),
        )

        assertTrue(result.amount > Money.of("22000.00").amount)
    }

    @Test
    fun `optimistic scenario should outperform conservative scenario`() {
        val simulator = CompoundInterestSimulator()
        val conservative = simulator.simulatePreset(Money.of("10000.00"), 15, RateDistribution.YEARLY, ScenarioPreset.CONSERVATIVE)
        val optimistic = simulator.simulatePreset(Money.of("10000.00"), 15, RateDistribution.YEARLY, ScenarioPreset.OPTIMISTIC)

        assertTrue(optimistic.amount > conservative.amount)
    }
}
