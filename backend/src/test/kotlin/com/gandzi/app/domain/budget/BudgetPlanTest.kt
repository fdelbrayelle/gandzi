package com.gandzi.app.domain.budget

import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetPlanTest {
    @Test
    fun `should apply rollover for leftover planned budget`() {
        val months = MonthKey.entries.mapIndexed { index, month ->
            when (index) {
                0 -> BudgetMonth(month, Money.of("100.00"), Money.of("80.00"))
                1 -> BudgetMonth(month, Money.of("100.00"), Money.of("90.00"))
                else -> BudgetMonth(month, Money.of("100.00"), Money.of("100.00"))
            }
        }

        val plan = BudgetPlan(
            year = 2026,
            dimension = BudgetDimension(BudgetDimensionType.CATEGORY, "groceries"),
            rolloverEnabled = true,
            months = months,
        )

        val rolled = plan.withRollover()

        assertEquals("120.00", rolled.months[1].planned.amount.toPlainString())
        assertEquals("130.00", rolled.months[2].planned.amount.toPlainString())
    }
}
