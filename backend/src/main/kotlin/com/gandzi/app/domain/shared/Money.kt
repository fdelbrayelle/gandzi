package com.gandzi.app.domain.shared

import java.math.BigDecimal

@JvmInline
value class Money private constructor(val amount: BigDecimal) {
    companion object {
        fun of(value: String): Money {
            val amount = BigDecimal(value)
            require(amount.scale() <= 2) { "Money supports at most 2 decimal places" }
            return Money(amount)
        }
    }

    operator fun plus(other: Money): Money = Money(this.amount + other.amount)

    operator fun minus(other: Money): Money = Money(this.amount - other.amount)
}
