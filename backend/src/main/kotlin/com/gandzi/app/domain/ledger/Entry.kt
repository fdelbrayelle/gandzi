package com.gandzi.app.domain.ledger

import com.gandzi.app.domain.shared.Money

data class EntryLine(
    val accountId: String,
    val amount: Money,
    val isDebit: Boolean,
)

data class Category(
    val name: String,
    val custom: Boolean,
)

object DefaultCategories {
    val values = setOf(
        "groceries", "transport", "health", "gifts", "vacation",
        "home_insurance", "borrower_insurance", "vehicle_insurance",
        "taxes", "personal_care", "car_maintenance",
        "subscriptions", "investments", "bank_fees", "mortgage",
        "rent", "property_tax", "electricity", "gas", "water",
        "miscellaneous", "leisure",
    )
}

data class Entry(
    val id: String,
    val lines: List<EntryLine>,
    val description: String,
    val category: Category,
) {
    init {
        require(lines.size >= 2) { "Double-entry requires at least two lines" }
        val debitTotal = lines.filter { it.isDebit }.fold(Money.of("0.00")) { acc, line -> acc + line.amount }
        val creditTotal = lines.filter { !it.isDebit }.fold(Money.of("0.00")) { acc, line -> acc + line.amount }
        require(debitTotal.amount.compareTo(creditTotal.amount) == 0) { "Entry must be balanced" }
    }
}
