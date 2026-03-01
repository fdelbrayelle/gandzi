package com.gandzi.app.domain.ledger

import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EntryTest {
    @Test
    fun `should reject unbalanced entry`() {
        assertFailsWith<IllegalArgumentException> {
            Entry(
                id = "e1",
                description = "invalid",
                category = EntryCategory.Default(DefaultEntryCategory.GROCERIES),
                lines = listOf(
                    EntryLine("cash", Money.of("10.00"), isDebit = true),
                    EntryLine("income", Money.of("9.00"), isDebit = false),
                ),
            )
        }
    }
}
