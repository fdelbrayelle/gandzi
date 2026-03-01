package com.gandzi.app.domain.wealth

import com.gandzi.app.domain.shared.Money
import java.time.Instant

enum class AssetCategory {
    CASH, EQUITY, BOND, ETF, CRYPTO, REAL_ESTATE, COMMODITY, PRIVATE_EQUITY, OTHER
}

enum class HoldingSupport {
    CASH_ACCOUNT, SAVINGS, PEA, CTO, LIFE_INSURANCE, RETIREMENT, CRYPTO_WALLET, OTHER
}

enum class SnapshotFrequency {
    DAILY, MONTHLY, YEARLY, ON_DEMAND
}

data class Asset(
    val id: String,
    val name: String,
    val category: AssetCategory,
    val support: HoldingSupport,
    val value: Money,
)

data class Liability(
    val id: String,
    val name: String,
    val amount: Money,
)

data class WealthSnapshot(
    val takenAt: Instant,
    val frequency: SnapshotFrequency,
    val assets: List<Asset>,
    val liabilities: List<Liability>,
) {
    fun financialWealth(): Money = assets
        .filter { it.category != AssetCategory.REAL_ESTATE }
        .fold(Money.of("0.00")) { acc, asset -> acc + asset.value }

    fun grossWealth(): Money = assets
        .fold(Money.of("0.00")) { acc, asset -> acc + asset.value }

    fun netWealth(): Money = grossWealth() - liabilities
        .fold(Money.of("0.00")) { acc, debt -> acc + debt.amount }
}
