package com.gandzi.app.application.ports

import com.gandzi.app.domain.budget.BudgetDimension
import com.gandzi.app.domain.budget.MonthKey

interface BudgetAlertNotifier {
    fun notifyThresholdReached(
        year: Int,
        month: MonthKey,
        dimension: BudgetDimension,
        ratio: Double,
    )
}
