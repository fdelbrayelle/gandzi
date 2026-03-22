package com.gandzi.app.application.ports

import java.math.BigDecimal

data class InvestmentLineDto(
    val lineId: String,
    val labelKey: String,
    val defaultValues: List<BigDecimal>,
)

interface BudgetRepository {
    fun getAllYearData(userId: String): Map<Int, Map<String, List<BigDecimal>>>
    fun saveYearLine(userId: String, year: Int, lineId: String, values: List<BigDecimal>)
    fun getInvestmentLines(userId: String): List<InvestmentLineDto>
    fun saveInvestmentLine(userId: String, line: InvestmentLineDto)
}
