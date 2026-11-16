package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_logs")
data class BlockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val actionType: String,
    val details: String
) {
    companion object {
        const val ACTION_INTERNET_BLOCKED = "INTERNET_BLOCKED"
        const val ACTION_MEMORY_FREED = "MEMORY_FREED"
        const val ACTION_IMMEDIATE_APPLIED = "IMMEDIATE_APPLIED"
        const val ACTION_SCHEDULE_CREATED = "SCHEDULE_CREATED"
        const val ACTION_SCHEDULE_STARTED = "SCHEDULE_STARTED"
        const val ACTION_SCHEDULE_ENDED = "SCHEDULE_ENDED"
        const val ACTION_UNBLOCKED = "UNBLOCKED"
    }
}
