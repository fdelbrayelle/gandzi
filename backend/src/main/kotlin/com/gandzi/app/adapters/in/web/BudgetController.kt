package com.gandzi.app.adapters.`in`.web

import com.gandzi.app.application.ports.BudgetRepository
import com.gandzi.app.application.ports.InvestmentLineDto
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

data class BudgetDataResponse(
    val yearData: Map<Int, Map<String, List<Double>>>,
    val investmentLines: List<InvestmentLineResponse>,
)

data class InvestmentLineResponse(
    val id: String,
    val labelKey: String,
    val defaultValues: List<Double>,
)

data class BudgetDataRequest(
    val yearData: Map<Int, Map<String, List<Double>>>,
    val investmentLines: List<InvestmentLineRequest>,
)

data class InvestmentLineRequest(
    val id: String,
    val labelKey: String,
    val defaultValues: List<Double>,
)

data class CellUpdateRequest(
    val year: Int,
    val lineId: String,
    val monthIdx: Int,
    val value: Double,
)

@RestController
@RequestMapping("/api/v1/budget")
class BudgetController(private val budgetRepository: BudgetRepository) {

    @GetMapping
    fun getBudget(@AuthenticationPrincipal jwt: Jwt): BudgetDataResponse {
        val userId = jwt.subject
        val yearData = budgetRepository.getAllYearData(userId)
            .mapValues { (_, lines) ->
                lines.mapValues { (_, values) -> values.map { it.toDouble() } }
            }
        val investmentLines = budgetRepository.getInvestmentLines(userId).map {
            InvestmentLineResponse(it.lineId, it.labelKey, it.defaultValues.map { v -> v.toDouble() })
        }
        return BudgetDataResponse(yearData, investmentLines)
    }

    @PutMapping
    fun saveBudget(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: BudgetDataRequest,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        for ((year, lines) in request.yearData) {
            for ((lineId, values) in lines) {
                budgetRepository.saveYearLine(userId, year, lineId, values.map { BigDecimal.valueOf(it) })
            }
        }
        for (line in request.investmentLines) {
            budgetRepository.saveInvestmentLine(userId, InvestmentLineDto(
                lineId = line.id,
                labelKey = line.labelKey,
                defaultValues = line.defaultValues.map { BigDecimal.valueOf(it) },
            ))
        }
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/cell")
    fun updateCell(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CellUpdateRequest,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        // Get existing year line, or create a new one with zeros
        val yearData = budgetRepository.getAllYearData(userId)
        val existing = yearData[request.year]?.get(request.lineId)?.toMutableList()
            ?: MutableList(12) { BigDecimal.ZERO }
        existing[request.monthIdx] = BigDecimal.valueOf(request.value)
        budgetRepository.saveYearLine(userId, request.year, request.lineId, existing)
        return ResponseEntity.noContent().build()
    }
}
