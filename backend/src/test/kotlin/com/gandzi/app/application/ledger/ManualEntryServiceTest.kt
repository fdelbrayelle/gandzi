package com.gandzi.app.application.ledger

import com.gandzi.app.application.ports.EntryRepository
import com.gandzi.app.domain.ledger.DefaultEntryCategory
import com.gandzi.app.domain.ledger.Entry
import com.gandzi.app.domain.ledger.EntryCategory
import com.gandzi.app.domain.ledger.EntryLine
import com.gandzi.app.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualEntryServiceTest {
    @Test
    fun `should save known category entries`() {
        val saved = mutableListOf<Entry>()
        val repo = object : EntryRepository {
            override fun save(entry: Entry) {
                saved.add(entry)
            }

            override fun delete(id: String) = Unit
        }

        val service = ManualEntryService(repo)
        val entry = Entry(
            id = "e2",
            description = "food",
            category = EntryCategory.Default(DefaultEntryCategory.GROCERIES),
            lines = listOf(
                EntryLine("expense", Money.of("50.00"), isDebit = true),
                EntryLine("cash", Money.of("50.00"), isDebit = false),
            ),
        )

        service.add(entry)

        assertEquals(1, saved.size)
    }

    @Test
    fun `should reject blank custom category name`() {
        assertFailsWith<IllegalArgumentException> {
            EntryCategory.Custom(" ")
        }
    }
}
