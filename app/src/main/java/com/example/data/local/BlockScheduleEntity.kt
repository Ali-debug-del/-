package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BlockMode

@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val packageNamesCsv: String,
    val appNamesPreview: String,
    val blockMode: BlockMode,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val hasEndTime: Boolean = true,
    val repeatDaily: Boolean = false,
    val daysMask: Int = ALL_DAYS_MASK,
    val isEnabled: Boolean = true,
    val isCurrentlyTriggered: Boolean = false,
    val nextStartMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun targetPackages(): List<String> =
        packageNamesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        // Bitmask for Calendar.SUNDAY (1) .. Calendar.SATURDAY (7) -> bit (dayOfWeek - 1)
        const val ALL_DAYS_MASK = 0b1111111
    }
}
