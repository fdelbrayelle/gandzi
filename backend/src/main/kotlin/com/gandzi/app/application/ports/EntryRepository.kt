package com.gandzi.app.application.ports

import com.gandzi.app.domain.ledger.Entry

interface EntryRepository {
    fun save(entry: Entry)
    fun delete(id: String)
}
