package com.gandzi.app.domain.realestate

import com.gandzi.app.domain.shared.Money
import java.math.BigDecimal
import java.math.MathContext

data class PrimaryResidence(
    val id: String,
    val name: String,
    val valuation: Money,
    val ownershipSharePercent: Double,
) {
    init {
        require(ownershipSharePercent in 0.0..100.0) { "Ownership share must be in [0, 100]" }
    }

    fun ownedValue(): Money {
        val ratio = BigDecimal.valueOf(ownershipSharePercent).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
        val value = valuation.amount.multiply(ratio)
        return Money.of(value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
    }
}

data class FixedRateMortgage(
    val principal: Money,
    val annualRatePercent: Double,
    val durationMonths: Int,
) {
    init {
        require(durationMonths > 0) { "Duration must be > 0" }
        require(annualRatePercent >= 0.0) { "Rate must be >= 0" }
    }

    fun monthlyPayment(): Money {
        val monthlyRate = BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(1200), MathContext.DECIMAL64)
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return Money.of(
                principal.amount
                    .divide(BigDecimal.valueOf(durationMonths.toLong()), 2, java.math.RoundingMode.HALF_UP)
                    .toPlainString(),
            )
        }

        val onePlusRatePowN = (BigDecimal.ONE + monthlyRate).pow(durationMonths, MathContext.DECIMAL64)
        val numerator = principal.amount.multiply(monthlyRate).multiply(onePlusRatePowN)
        val denominator = onePlusRatePowN - BigDecimal.ONE
        return Money.of(numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP).toPlainString())
    }

    fun outstandingPrincipal(afterMonths: Int): Money {
        require(afterMonths in 0..durationMonths) { "afterMonths out of bounds" }
        if (afterMonths == 0) return principal

        val monthlyRate = BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(1200), MathContext.DECIMAL64)
        val payment = monthlyPayment().amount

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            val remaining = principal.amount - payment.multiply(BigDecimal.valueOf(afterMonths.toLong()))
            return Money.of(remaining.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        }

        val factor = (BigDecimal.ONE + monthlyRate).pow(afterMonths, MathContext.DECIMAL64)
        val remaining = principal.amount.multiply(factor) - payment.multiply(factor - BigDecimal.ONE).divide(monthlyRate, MathContext.DECIMAL64)
        return Money.of(remaining.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
    }
}
