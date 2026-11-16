package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_telemetry")
data class MemoryTelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val totalRamMb: Long,
    val usedRamMb: Long,
    val availableRamMb: Long,
    val blockedAppsRamMb: Double,
    val blockedAppsCount: Int,
    val freedDeltaMb: Long = 0L,
    val eventType: String = EVENT_SAMPLE
) {
    companion object {
        const val EVENT_SAMPLE = "SAMPLE"
        const val EVENT_SWEEP = "SWEEP"
        const val EVENT_BLOCK_APPLIED = "BLOCK_APPLIED"
    }
}
