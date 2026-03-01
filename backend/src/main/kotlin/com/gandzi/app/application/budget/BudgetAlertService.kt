package com.gandzi.app.application.budget

import com.gandzi.app.application.ports.BudgetAlertNotifier
import com.gandzi.app.domain.budget.BudgetPlan

class BudgetAlertService(
    private val notifier: BudgetAlertNotifier,
    private val threshold: Double = 0.8,
) {
    fun evaluate(plan: BudgetPlan) {
        plan.months.forEach { month ->
            val ratio = month.usageRatio()
            if (ratio >= threshold) {
                notifier.notifyThresholdReached(plan.year, month.month, plan.dimension, ratio)
            }
        }
    }
}
