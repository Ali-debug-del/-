package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BlockMode

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val blockMode: BlockMode,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val blockedPacketsCount: Long = 0L,
    val memoryKillsCount: Int = 0
)
