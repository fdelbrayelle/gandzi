package com.gandzi.app.application.ledger

import com.gandzi.app.application.ports.EntryRepository
import com.gandzi.app.domain.ledger.DefaultCategories
import com.gandzi.app.domain.ledger.Entry

class ManualEntryService(
    private val repository: EntryRepository,
) {
    fun add(entry: Entry) {
        val isKnown = entry.category.custom || DefaultCategories.values.contains(entry.category.name)
        require(isKnown) { "Unknown category: ${entry.category.name}" }
        repository.save(entry)
    }

    fun delete(entryId: String) {
        repository.delete(entryId)
    }
}
