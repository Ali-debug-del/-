package com.example.data.model

data class MemorySnapshot(
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val usedRamMb: Long = 0L,
    val usagePercent: Float = 0f,
    val isLowMemory: Boolean = false,
    val runningBackgroundAppsCount: Int = 0,
    val lastFreedMb: Long = 0L
)
