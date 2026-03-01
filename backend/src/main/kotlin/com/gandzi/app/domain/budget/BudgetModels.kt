package com.gandzi.app.domain.budget

import com.gandzi.app.domain.shared.Money

enum class BudgetDimensionType { CATEGORY, ACCOUNT }

data class BudgetDimension(
    val type: BudgetDimensionType,
    val key: String,
)

enum class MonthKey {
    JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC
}

data class BudgetMonth(
    val month: MonthKey,
    val planned: Money,
    val actual: Money,
) {
    fun usageRatio(): Double {
        if (planned.amount.compareTo(java.math.BigDecimal.ZERO) == 0) return 0.0
        return actual.amount.divide(planned.amount, 4, java.math.RoundingMode.HALF_UP).toDouble()
    }
}

data class BudgetPlan(
    val year: Int,
    val dimension: BudgetDimension,
    val rolloverEnabled: Boolean,
    val months: List<BudgetMonth>,
) {
    init {
        require(months.size == 12) { "Budget plan must include 12 months" }
    }

    fun withRollover(): BudgetPlan {
        if (!rolloverEnabled) return this
        var carry = Money.of("0.00")
        val updated = months.map { month ->
            val adjustedPlanned = month.planned + carry
            val delta = adjustedPlanned - month.actual
            carry = if (delta.amount.signum() > 0) delta else Money.of("0.00")
            month.copy(planned = adjustedPlanned)
        }
        return copy(months = updated)
    }
}
