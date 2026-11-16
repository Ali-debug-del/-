package com.example.data.model

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val isRunningInBackground: Boolean,
    val estimatedRamMb: Double,
    val versionName: String,
    val blockMode: BlockMode = BlockMode.NONE,
    val isRuleActive: Boolean = false,
    val scheduledCount: Int = 0
)
