package com.gandzi.app.domain.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test
    fun `should add money amounts`() {
        val left = Money.of("10.00")
        val right = Money.of("5.50")

        assertEquals("15.50", (left + right).amount.toPlainString())
    }

    @Test
    fun `should reject too many decimal places`() {
        assertFailsWith<IllegalArgumentException> {
            Money.of("1.001")
        }
    }
}
