package com.gandzi.app.domain.ledger

import com.gandzi.app.domain.shared.Money

data class EntryLine(
    val accountId: String,
    val amount: Money,
    val isDebit: Boolean,
)

enum class DefaultEntryCategory {
    GROCERIES,
    TRANSPORT,
    HEALTH,
    GIFTS,
    VACATION,
    HOME_INSURANCE,
    BORROWER_INSURANCE,
    VEHICLE_INSURANCE,
    TAXES,
    PERSONAL_CARE,
    CAR_MAINTENANCE,
    SUBSCRIPTIONS,
    INVESTMENTS,
    BANK_FEES,
    MORTGAGE,
    RENT,
    PROPERTY_TAX,
    ELECTRICITY,
    GAS,
    WATER,
    MISCELLANEOUS,
    LEISURE,
}

sealed interface EntryCategory {
    data class Default(val value: DefaultEntryCategory) : EntryCategory
    data class Custom(val name: String) : EntryCategory {
        init {
            require(name.isNotBlank()) { "Custom category name cannot be blank" }
        }
    }
}

data class Entry(
    val id: String,
    val lines: List<EntryLine>,
    val description: String,
    val category: EntryCategory,
) {
    init {
        require(lines.size >= 2) { "Double-entry requires at least two lines" }
        val debitTotal = lines.filter { it.isDebit }.fold(Money.of("0.00")) { acc, line -> acc + line.amount }
        val creditTotal = lines.filter { !it.isDebit }.fold(Money.of("0.00")) { acc, line -> acc + line.amount }
        require(debitTotal.amount.compareTo(creditTotal.amount) == 0) { "Entry must be balanced" }
    }
}
