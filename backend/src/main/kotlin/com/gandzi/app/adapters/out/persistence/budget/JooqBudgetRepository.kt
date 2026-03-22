package com.gandzi.app.adapters.out.persistence.budget

import com.gandzi.app.application.ports.BudgetRepository
import com.gandzi.app.application.ports.InvestmentLineDto
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
class JooqBudgetRepository(private val dsl: DSLContext) : BudgetRepository {

    override fun getAllYearData(userId: String): Map<Int, Map<String, List<BigDecimal>>> {
        val result = mutableMapOf<Int, MutableMap<String, List<BigDecimal>>>()
        dsl.fetch(
            "SELECT year, line_id, monthly_values FROM budget_cell WHERE user_id = ? ORDER BY year, line_id",
            userId
        ).forEach { record ->
            val year = record.get("year", Int::class.java)
            val lineId = record.get("line_id", String::class.java)
            val values = (record.get("monthly_values") as Array<*>).map {
                when (it) {
                    is BigDecimal -> it
                    is Number -> BigDecimal.valueOf(it.toDouble())
                    else -> BigDecimal.ZERO
                }
            }
            result.getOrPut(year) { mutableMapOf() }[lineId] = values
        }
        return result
    }

    override fun saveYearLine(userId: String, year: Int, lineId: String, values: List<BigDecimal>) {
        require(values.size == 12) { "values must have exactly 12 elements" }
        val arrayLiteral = values.joinToString(",") { it.toPlainString() }
        dsl.execute(
            """
            INSERT INTO budget_cell (id, user_id, year, line_id, monthly_values, updated_at)
            VALUES (?, ?, ?, ?, ARRAY[$arrayLiteral]::numeric(12,2)[], CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, year, line_id) DO UPDATE
            SET monthly_values = ARRAY[$arrayLiteral]::numeric(12,2)[], updated_at = CURRENT_TIMESTAMP
            """.trimIndent(),
            UUID.randomUUID().toString(), userId, year.toShort(), lineId
        )
    }

    override fun getInvestmentLines(userId: String): List<InvestmentLineDto> {
        return dsl.fetch(
            "SELECT line_id, label_key, default_values FROM budget_investment_line WHERE user_id = ? ORDER BY line_id",
            userId
        ).map { record ->
            val defaultValues = (record.get("default_values") as Array<*>).map {
                when (it) {
                    is BigDecimal -> it
                    is Number -> BigDecimal.valueOf(it.toDouble())
                    else -> BigDecimal.ZERO
                }
            }
            InvestmentLineDto(
                lineId = record.get("line_id", String::class.java),
                labelKey = record.get("label_key", String::class.java),
                defaultValues = defaultValues,
            )
        }
    }

    override fun saveInvestmentLine(userId: String, line: InvestmentLineDto) {
        require(line.defaultValues.size == 12) { "defaultValues must have exactly 12 elements" }
        val arrayLiteral = line.defaultValues.joinToString(",") { it.toPlainString() }
        dsl.execute(
            """
            INSERT INTO budget_investment_line (id, user_id, line_id, label_key, default_values, created_at)
            VALUES (?, ?, ?, ?, ARRAY[$arrayLiteral]::numeric(12,2)[], CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, line_id) DO UPDATE
            SET label_key = ?, default_values = ARRAY[$arrayLiteral]::numeric(12,2)[]
            """.trimIndent(),
            UUID.randomUUID().toString(), userId, line.lineId, line.labelKey, line.labelKey
        )
    }
}
