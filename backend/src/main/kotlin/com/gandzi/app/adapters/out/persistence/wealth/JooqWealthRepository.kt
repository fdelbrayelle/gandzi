package com.gandzi.app.adapters.out.persistence.wealth

import com.gandzi.app.application.ports.AssetEntryDto
import com.gandzi.app.application.ports.WealthRepository
import com.gandzi.app.application.ports.WealthSnapshotDto
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class JooqWealthRepository(private val dsl: DSLContext) : WealthRepository {

    override fun getSnapshots(userId: String): List<WealthSnapshotDto> {
        val snapshots = dsl.fetch(
            "SELECT id, snapshot_date, liabilities FROM wealth_snapshot WHERE user_id = ? ORDER BY snapshot_date",
            userId
        )

        return snapshots.map { snap ->
            val snapshotId = snap.get("id", String::class.java)
            val assets = dsl.fetch(
                "SELECT support, value FROM wealth_snapshot_asset WHERE snapshot_id = ? ORDER BY support",
                snapshotId
            ).map { asset ->
                AssetEntryDto(
                    support = asset.get("support", String::class.java),
                    value = asset.get("value", BigDecimal::class.java),
                )
            }
            WealthSnapshotDto(
                date = snap.get("snapshot_date", LocalDate::class.java),
                assets = assets,
                liabilities = snap.get("liabilities", BigDecimal::class.java),
            )
        }
    }

    @Transactional
    override fun saveSnapshot(userId: String, snapshot: WealthSnapshotDto) {
        // Upsert snapshot
        val existingId = dsl.fetchOne(
            "SELECT id FROM wealth_snapshot WHERE user_id = ? AND snapshot_date = ?",
            userId, snapshot.date
        )?.get("id", String::class.java)

        val snapshotId = existingId ?: UUID.randomUUID().toString()

        if (existingId != null) {
            dsl.execute(
                "UPDATE wealth_snapshot SET liabilities = ? WHERE id = ?",
                snapshot.liabilities, snapshotId
            )
            dsl.execute("DELETE FROM wealth_snapshot_asset WHERE snapshot_id = ?", snapshotId)
        } else {
            dsl.execute(
                "INSERT INTO wealth_snapshot (id, user_id, snapshot_date, liabilities) VALUES (?, ?, ?, ?)",
                snapshotId, userId, snapshot.date, snapshot.liabilities
            )
        }

        // Insert assets
        for (asset in snapshot.assets) {
            dsl.execute(
                "INSERT INTO wealth_snapshot_asset (id, snapshot_id, support, value) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), snapshotId, asset.support, asset.value
            )
        }
    }

    @Transactional
    override fun deleteSnapshot(userId: String, date: LocalDate) {
        val snapshotId = dsl.fetchOne(
            "SELECT id FROM wealth_snapshot WHERE user_id = ? AND snapshot_date = ?",
            userId, date
        )?.get("id", String::class.java) ?: return

        dsl.execute("DELETE FROM wealth_snapshot_asset WHERE snapshot_id = ?", snapshotId)
        dsl.execute("DELETE FROM wealth_snapshot WHERE id = ?", snapshotId)
    }

    override fun getCustomSupports(userId: String): List<String> {
        return dsl.fetch(
            "SELECT support_name FROM wealth_custom_support WHERE user_id = ? ORDER BY support_name",
            userId
        ).map { it.get("support_name", String::class.java) }
    }

    override fun saveCustomSupport(userId: String, support: String) {
        dsl.execute(
            """
            INSERT INTO wealth_custom_support (id, user_id, support_name)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, support_name) DO NOTHING
            """.trimIndent(),
            UUID.randomUUID().toString(), userId, support
        )
    }
}
