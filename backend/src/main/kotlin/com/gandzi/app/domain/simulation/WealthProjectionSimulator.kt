package com.gandzi.app.domain.simulation

import com.gandzi.app.domain.shared.Money
import java.math.BigDecimal
import java.math.MathContext
import java.util.Random

data class WealthProjectionInput(
    val initialWealth: Money,
    val annualIncome: Money,
    val annualExpenses: Money,
    val years: Int,
    val expectedAnnualReturnPercent: Double,
    val inflationPercent: Double,
    val incomeGrowthPercent: Double,
    val expensesGrowthPercent: Double,
) {
    init {
        require(years in 1..100) { "Projection years must be within [1, 100]" }
    }
}

data class WealthProjectionResult(
    val yearlyValues: List<Money>,
)

data class MonteCarloResult(
    val p10: Money,
    val p50: Money,
    val p90: Money,
)

class WealthProjectionSimulator(
    private val random: Random = Random(42),
) {
    fun deterministic(input: WealthProjectionInput): WealthProjectionResult {
        var wealth = input.initialWealth.amount
        var income = input.annualIncome.amount
        var expenses = input.annualExpenses.amount

        val values = mutableListOf<Money>()

        repeat(input.years) {
            val returnRate = BigDecimal.valueOf(input.expectedAnnualReturnPercent / 100)
            wealth = wealth.multiply(BigDecimal.ONE + returnRate, MathContext.DECIMAL64)
            wealth = wealth + income - expenses

            val inflationFactor = BigDecimal.ONE + BigDecimal.valueOf(input.inflationPercent / 100)
            income = income.multiply(BigDecimal.ONE + BigDecimal.valueOf(input.incomeGrowthPercent / 100), MathContext.DECIMAL64)
            expenses = expenses.multiply(BigDecimal.ONE + BigDecimal.valueOf(input.expensesGrowthPercent / 100), MathContext.DECIMAL64)
            expenses = expenses.multiply(inflationFactor, MathContext.DECIMAL64)

            values.add(Money.of(wealth.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))
        }

        return WealthProjectionResult(values)
    }

    fun monteCarlo(input: WealthProjectionInput, trials: Int = 1000, volatilityPercent: Double = 12.0): MonteCarloResult {
        require(trials > 0) { "Trials must be positive" }
        val finals = mutableListOf<BigDecimal>()

        repeat(trials) {
            var wealth = input.initialWealth.amount
            var income = input.annualIncome.amount
            var expenses = input.annualExpenses.amount

            repeat(input.years) {
                val shock = random.nextGaussian() * (volatilityPercent / 100)
                val annualRate = (input.expectedAnnualReturnPercent / 100) + shock
                wealth = wealth.multiply(BigDecimal.ONE + BigDecimal.valueOf(annualRate), MathContext.DECIMAL64)
                wealth = wealth + income - expenses

                income = income.multiply(BigDecimal.ONE + BigDecimal.valueOf(input.incomeGrowthPercent / 100), MathContext.DECIMAL64)
                expenses = expenses.multiply(BigDecimal.ONE + BigDecimal.valueOf((input.expensesGrowthPercent + input.inflationPercent) / 100), MathContext.DECIMAL64)
            }

            finals.add(wealth)
        }

        val sorted = finals.sorted()
        fun percentile(p: Double): Money {
            val index = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
            return Money.of(sorted[index].setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        }

        return MonteCarloResult(
            p10 = percentile(0.10),
            p50 = percentile(0.50),
            p90 = percentile(0.90),
        )
    }
}
