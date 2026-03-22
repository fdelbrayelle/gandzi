package com.gandzi.app.adapters.`in`.web

import com.gandzi.app.application.ports.AssetEntryDto
import com.gandzi.app.application.ports.WealthRepository
import com.gandzi.app.application.ports.WealthSnapshotDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

data class WealthDataResponse(
    val snapshots: List<SnapshotResponse>,
    val customSupports: List<String>,
)

data class SnapshotResponse(
    val date: String,
    val assets: List<AssetResponse>,
    val liabilities: Double,
)

data class AssetResponse(val support: String, val value: Double)

data class WealthDataRequest(
    val snapshots: List<SnapshotRequest>,
    val customSupports: List<String>,
)

data class SnapshotRequest(
    val date: String,
    val assets: List<AssetRequest>,
    val liabilities: Double,
)

data class AssetRequest(val support: String, val value: Double)

data class CustomSupportRequest(val name: String)

@RestController
@RequestMapping("/api/v1/wealth")
class WealthController(private val wealthRepository: WealthRepository) {

    @GetMapping
    fun getWealth(@AuthenticationPrincipal jwt: Jwt): WealthDataResponse {
        val userId = jwt.subject
        val snapshots = wealthRepository.getSnapshots(userId).map { snap ->
            SnapshotResponse(
                date = snap.date.toString(),
                assets = snap.assets.map { AssetResponse(it.support, it.value.toDouble()) },
                liabilities = snap.liabilities.toDouble(),
            )
        }
        val customSupports = wealthRepository.getCustomSupports(userId)
        return WealthDataResponse(snapshots, customSupports)
    }

    @PutMapping
    fun saveWealth(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: WealthDataRequest,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        for (snap in request.snapshots) {
            wealthRepository.saveSnapshot(userId, WealthSnapshotDto(
                date = LocalDate.parse(snap.date),
                assets = snap.assets.map { AssetEntryDto(it.support, BigDecimal.valueOf(it.value)) },
                liabilities = BigDecimal.valueOf(snap.liabilities),
            ))
        }
        for (support in request.customSupports) {
            wealthRepository.saveCustomSupport(userId, support)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/snapshots")
    fun addSnapshot(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: SnapshotRequest,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        wealthRepository.saveSnapshot(userId, WealthSnapshotDto(
            date = LocalDate.parse(request.date),
            assets = request.assets.map { AssetEntryDto(it.support, BigDecimal.valueOf(it.value)) },
            liabilities = BigDecimal.valueOf(request.liabilities),
        ))
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/snapshots/{date}")
    fun deleteSnapshot(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable date: String,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        wealthRepository.deleteSnapshot(userId, LocalDate.parse(date))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/supports")
    fun addCustomSupport(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CustomSupportRequest,
    ): ResponseEntity<Void> {
        val userId = jwt.subject
        wealthRepository.saveCustomSupport(userId, request.name)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
