package com.gandzi.app.domain.simulation

import com.gandzi.app.domain.shared.Money
import java.math.BigDecimal
import java.math.MathContext

enum class RateDistribution(val periodsPerYear: Int) {
    MONTHLY(12), QUARTERLY(4), YEARLY(1)
}

enum class ScenarioPreset(val annualRatePercent: Double) {
    CONSERVATIVE(3.0), BASE(6.0), OPTIMISTIC(9.0)
}

data class CompoundInterestInput(
    val principal: Money,
    val years: Int,
    val distribution: RateDistribution,
    val annualRatePercent: Double,
    val recurringContribution: Money = Money.of("0.00"),
)

class CompoundInterestSimulator {
    fun simulate(input: CompoundInterestInput): Money {
        val periods = input.years * input.distribution.periodsPerYear
        val periodicRate = BigDecimal.valueOf(input.annualRatePercent)
            .divide(BigDecimal.valueOf(100L * input.distribution.periodsPerYear), MathContext.DECIMAL64)

        var value = input.principal.amount
        repeat(periods) {
            value = value.multiply(BigDecimal.ONE + periodicRate, MathContext.DECIMAL64)
            value = value + input.recurringContribution.amount
        }
        return Money.of(value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    fun simulatePreset(
        principal: Money,
        years: Int,
        distribution: RateDistribution,
        preset: ScenarioPreset,
        recurringContribution: Money = Money.of("0.00"),
    ): Money {
        return simulate(
            CompoundInterestInput(
                principal = principal,
                years = years,
                distribution = distribution,
                annualRatePercent = preset.annualRatePercent,
                recurringContribution = recurringContribution,
            ),
        )
    }
}
