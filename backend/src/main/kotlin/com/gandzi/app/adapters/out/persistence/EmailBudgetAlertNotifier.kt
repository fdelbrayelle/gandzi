package com.gandzi.app.adapters.out.persistence

import com.gandzi.app.application.ports.BudgetAlertNotifier
import com.gandzi.app.domain.budget.BudgetDimension
import com.gandzi.app.domain.budget.MonthKey
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmailBudgetAlertNotifier : BudgetAlertNotifier {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun notifyThresholdReached(year: Int, month: MonthKey, dimension: BudgetDimension, ratio: Double) {
        logger.info(
            "Budget threshold reached year={}, month={}, dimensionType={}, dimensionKey={}, ratio={}",
            year,
            month,
            dimension.type,
            dimension.key,
            ratio,
        )
    }
}
