package com.gandzi.app.application.budget

import com.gandzi.app.application.ports.BudgetAlertNotifier
import com.gandzi.app.domain.budget.BudgetDimension
import com.gandzi.app.domain.budget.BudgetDimensionType
import com.gandzi.app.domain.budget.BudgetMonth
import com.gandzi.app.domain.budget.BudgetPlan
import com.gandzi.app.domain.budget.MonthKey
import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetAlertServiceTest {
    @Test
    fun `should notify when threshold reached`() {
        val calls = mutableListOf<MonthKey>()
        val notifier = object : BudgetAlertNotifier {
            override fun notifyThresholdReached(year: Int, month: MonthKey, dimension: BudgetDimension, ratio: Double) {
                calls.add(month)
            }
        }

        val plan = BudgetPlan(
            year = 2026,
            dimension = BudgetDimension(BudgetDimensionType.ACCOUNT, "checking"),
            rolloverEnabled = false,
            months = MonthKey.entries.map { month ->
                BudgetMonth(month, Money.of("100.00"), if (month == MonthKey.JAN) Money.of("80.00") else Money.of("20.00"))
            },
        )

        BudgetAlertService(notifier, threshold = 0.8).evaluate(plan)

        assertEquals(listOf(MonthKey.JAN), calls)
    }
}
