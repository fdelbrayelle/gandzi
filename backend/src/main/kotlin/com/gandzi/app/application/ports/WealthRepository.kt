package com.gandzi.app.application.ports

import java.math.BigDecimal
import java.time.LocalDate

data class AssetEntryDto(
    val support: String,
    val value: BigDecimal,
)

data class WealthSnapshotDto(
    val date: LocalDate,
    val assets: List<AssetEntryDto>,
    val liabilities: BigDecimal,
)

interface WealthRepository {
    fun getSnapshots(userId: String): List<WealthSnapshotDto>
    fun saveSnapshot(userId: String, snapshot: WealthSnapshotDto)
    fun deleteSnapshot(userId: String, date: LocalDate)
    fun getCustomSupports(userId: String): List<String>
    fun saveCustomSupport(userId: String, support: String)
}
