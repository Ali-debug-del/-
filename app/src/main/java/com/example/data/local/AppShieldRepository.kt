package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.data.model.BlockMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.MemorySnapshot
import com.example.system.FirewallStateHolder
import com.example.system.FirewallVpnService
import com.example.system.InstalledAppsProvider
import com.example.system.MemoryGuardManager
import com.example.system.MemorySweepResult
import com.example.system.ScheduleAlarmManager
import kotlinx.coroutines.flow.Flow

class AppShieldRepository(private val context: Context) {

    private val dao = AppShieldDatabase.getInstance(context).appShieldDao()
    val appsProvider = InstalledAppsProvider(context)
    val memoryGuard = MemoryGuardManager(context)
    private val alarmManager = ScheduleAlarmManager(context)

    fun observeRules(): Flow<List<AppRuleEntity>> = dao.observeAllRules()

    fun observeSchedules(): Flow<List<BlockScheduleEntity>> = dao.observeAllSchedules()

    fun observeLogs(): Flow<List<BlockLogEntity>> = dao.observeRecentLogs()

    fun observeMemoryTelemetry(): Flow<List<MemoryTelemetryEntity>> = dao.observeMemoryTelemetry()

    fun observeTotalFreedMb(): Flow<Long> = dao.observeTotalFreedMb()

    suspend fun loadInstalledApps(): List<InstalledAppInfo> = appsProvider.loadInstalledApps()

    fun getMemorySnapshot(): MemorySnapshot = memoryGuard.getMemorySnapshot()

    fun prepareVpnIntent(): Intent? = VpnService.prepare(context)

    fun hasUsageStatsPermission(): Boolean = memoryGuard.hasUsageStatsPermission()

    fun openUsageAccessSettings() = memoryGuard.openUsageAccessSettings()

    fun openAppDetailsSettings(packageName: String) = memoryGuard.openAppDetailsSettings(packageName)

    suspend fun applyImmediateBlock(
        apps: List<InstalledAppInfo>,
        blockMode: BlockMode
    ): ApplyBlockOutcome {
        if (apps.isEmpty()) return ApplyBlockOutcome(needsVpnConsent = null, sweepResult = null)

        val now = System.currentTimeMillis()
        if (blockMode == BlockMode.NONE) {
            for (app in apps) {
                dao.deleteRule(app.packageName)
                dao.insertLog(
                    BlockLogEntity(
                        packageName = app.packageName,
                        appName = app.appName,
                        actionType = BlockLogEntity.ACTION_UNBLOCKED,
                        details = "تم إلغاء الحجب فوراً"
                    )
                )
            }
            syncServiceWithCurrentRules()
            return ApplyBlockOutcome(needsVpnConsent = null, sweepResult = null)
        }

        val rules = mutableListOf<AppRuleEntity>()
        for (app in apps) {
            val existing = dao.getRuleByPackageSync(app.packageName)
            rules.add(
                AppRuleEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    blockMode = blockMode,
                    isActive = true,
                    updatedAt = now,
                    blockedPacketsCount = existing?.blockedPacketsCount ?: 0L,
                    memoryKillsCount = existing?.memoryKillsCount ?: 0
                )
            )
        }
        dao.upsertRules(rules)

        var sweepResult: MemorySweepResult? = null
        if (blockMode.blocksMemory) {
            val pkgs = apps.map { it.packageName }
            sweepResult = memoryGuard.enforceMemoryBlock(pkgs)
            dao.incrementMemoryKills(pkgs, now)
            dao.insertLog(
                BlockLogEntity(
                    packageName = pkgs.first(),
                    appName = if (apps.size == 1) apps.first().appName else "${apps.size} تطبيقات",
                    actionType = BlockLogEntity.ACTION_MEMORY_FREED,
                    details = "تقييد فوري للذاكرة (${sweepResult.targetedCount} تطبيق) • تم تحرير ${sweepResult.freedMb} MB"
                )
            )
            recordTelemetrySnapshot(
                blockedAppsRamMb = apps.sumOf { it.estimatedRamMb },
                blockedAppsCount = dao.getAllRulesSync().count { it.isActive && it.blockMode.isBlocked },
                freedDeltaMb = sweepResult.freedMb,
                eventType = MemoryTelemetryEntity.EVENT_BLOCK_APPLIED,
                snapshotOverride = sweepResult.snapshotAfter
            )
        }

        dao.insertLog(
            BlockLogEntity(
                packageName = apps.first().packageName,
                appName = if (apps.size == 1) apps.first().appName else "${apps.size} تطبيقات محددة",
                actionType = BlockLogEntity.ACTION_IMMEDIATE_APPLIED,
                details = "تطبيق فوري: ${blockMode.displayLabelAr()}"
            )
        )

        val vpnIntent = if (blockMode.blocksInternet) prepareVpnIntent() else null
        if (vpnIntent == null) {
            syncServiceWithCurrentRules()
        }
        return ApplyBlockOutcome(needsVpnConsent = vpnIntent, sweepResult = sweepResult)
    }

    suspend fun createSchedule(
        title: String,
        apps: List<InstalledAppInfo>,
        blockMode: BlockMode,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        hasEndTime: Boolean,
        repeatDaily: Boolean,
        daysMask: Int
    ): Long {
        val packagesCsv = apps.joinToString(",") { it.packageName }
        val namesPreview = when {
            apps.isEmpty() -> ""
            apps.size <= 2 -> apps.joinToString("، ") { it.appName }
            else -> "${apps[0].appName}، ${apps[1].appName} +${apps.size - 2}"
        }
        val cleanTitle = title.ifBlank {
            "حجب ${blockMode.displayLabelAr()} (${apps.size})"
        }

        val initialEntity = BlockScheduleEntity(
            title = cleanTitle,
            packageNamesCsv = packagesCsv,
            appNamesPreview = namesPreview,
            blockMode = blockMode,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            hasEndTime = hasEndTime,
            repeatDaily = repeatDaily,
            daysMask = daysMask,
            isEnabled = true
        )
        val newId = dao.insertSchedule(initialEntity)
        val saved = initialEntity.copy(id = newId)
        val nextStart = alarmManager.scheduleAlarms(saved)
        dao.updateSchedule(saved.copy(nextStartMillis = nextStart))

        dao.insertLog(
            BlockLogEntity(
                packageName = apps.firstOrNull()?.packageName ?: "",
                appName = cleanTitle,
                actionType = BlockLogEntity.ACTION_SCHEDULE_CREATED,
                details = "تمت جدولة ${apps.size} تطبيق (${blockMode.displayLabelAr()}) عند %02d:%02d".format(
                    startHour,
                    startMinute
                )
            )
        )
        return newId
    }

    suspend fun toggleScheduleEnabled(schedule: BlockScheduleEntity, enabled: Boolean) {
        val updated = schedule.copy(isEnabled = enabled)
        if (enabled) {
            val nextStart = alarmManager.scheduleAlarms(updated)
            dao.updateSchedule(updated.copy(nextStartMillis = nextStart))
        } else {
            alarmManager.cancelAlarms(schedule.id)
            dao.updateSchedule(updated.copy(isCurrentlyTriggered = false))
        }
    }

    suspend fun deleteSchedule(schedule: BlockScheduleEntity) {
        alarmManager.cancelAlarms(schedule.id)
        dao.deleteScheduleById(schedule.id)
    }

    suspend fun runScheduleNow(schedule: BlockScheduleEntity, allApps: List<InstalledAppInfo>): ApplyBlockOutcome {
        val pkgs = schedule.targetPackages().toSet()
        val matched = allApps.filter { it.packageName in pkgs }.ifEmpty {
            pkgs.map { pkg ->
                InstalledAppInfo(
                    packageName = pkg,
                    appName = pkg,
                    isSystemApp = false,
                    hasInternetPermission = true,
                    isRunningInBackground = false,
                    estimatedRamMb = 0.0,
                    versionName = "1.0"
                )
            }
        }
        dao.updateSchedule(schedule.copy(isCurrentlyTriggered = true))
        return applyImmediateBlock(matched, schedule.blockMode)
    }

    suspend fun toggleMasterShield(enable: Boolean): Intent? {
        FirewallStateHolder.setMasterEnabled(enable)
        val rules = dao.getAllRulesSync()
        if (enable) {
            val updated = rules.map { it.copy(isActive = true) }
            if (updated.isNotEmpty()) dao.upsertRules(updated)
            val needsVpn = updated.any { it.blockMode.blocksInternet }
            val consent = if (needsVpn) prepareVpnIntent() else null
            if (consent == null && updated.isNotEmpty()) {
                FirewallVpnService.startOrUpdate(context)
            }
            return consent
        } else {
            val updated = rules.map { it.copy(isActive = false) }
            if (updated.isNotEmpty()) dao.upsertRules(updated)
            FirewallVpnService.stop(context)
            return null
        }
    }

    suspend fun enforceMemorySweepAll(): MemorySweepResult {
        val activeMemRules = dao.getAllRulesSync()
            .filter { it.isActive && it.blockMode.blocksMemory }
        val pkgs = activeMemRules.map { it.packageName }
        val installedNow = loadInstalledApps()
        val blockedSet = pkgs.toSet()
        val blockedRamBefore = installedNow
            .filter { it.packageName in blockedSet }
            .sumOf { it.estimatedRamMb }

        val result = memoryGuard.enforceMemoryBlock(pkgs)
        if (pkgs.isNotEmpty()) {
            dao.incrementMemoryKills(pkgs)
            dao.insertLog(
                BlockLogEntity(
                    packageName = pkgs.first(),
                    appName = "${pkgs.size} تطبيقات الذاكرة",
                    actionType = BlockLogEntity.ACTION_MEMORY_FREED,
                    details = "فحص وتنظيف الذاكرة: استهداف ${result.targetedCount} تطبيق، تحرير ${result.freedMb} MB"
                )
            )
            recordTelemetrySnapshot(
                blockedAppsRamMb = (blockedRamBefore - result.freedMb).coerceAtLeast(0.0),
                blockedAppsCount = dao.getAllRulesSync().count { it.isActive && it.blockMode.isBlocked },
                freedDeltaMb = result.freedMb,
                eventType = MemoryTelemetryEntity.EVENT_SWEEP,
                snapshotOverride = result.snapshotAfter
            )
        }
        return result
    }

    suspend fun recordTelemetrySnapshot(
        blockedAppsRamMb: Double,
        blockedAppsCount: Int,
        freedDeltaMb: Long = 0L,
        eventType: String = MemoryTelemetryEntity.EVENT_SAMPLE,
        snapshotOverride: MemorySnapshot? = null
    ) {
        val snap = snapshotOverride ?: getMemorySnapshot()
        dao.insertTelemetry(
            MemoryTelemetryEntity(
                timestamp = System.currentTimeMillis(),
                totalRamMb = snap.totalRamMb,
                usedRamMb = snap.usedRamMb,
                availableRamMb = snap.availableRamMb,
                blockedAppsRamMb = blockedAppsRamMb,
                blockedAppsCount = blockedAppsCount,
                freedDeltaMb = freedDeltaMb,
                eventType = eventType
            )
        )
        dao.pruneOldTelemetry()
    }

    suspend fun syncServiceWithCurrentRules() {
        val rules = dao.getAllRulesSync().filter { it.isActive }
        val internetCount = rules.count { it.blockMode.blocksInternet }
        val memoryCount = rules.count { it.blockMode.blocksMemory }
        FirewallStateHolder.updateRuleCounts(internetCount, memoryCount)

        if (internetCount == 0 && memoryCount == 0) {
            FirewallVpnService.stop(context)
        } else if (internetCount > 0 && prepareVpnIntent() == null) {
            FirewallVpnService.startOrUpdate(context)
        } else if (internetCount == 0 && memoryCount > 0) {
            FirewallVpnService.startOrUpdate(context)
        }
    }

    suspend fun clearAllRules() {
        dao.clearAllRules()
        FirewallVpnService.stop(context)
        FirewallStateHolder.updateRuleCounts(0, 0)
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }
}

data class ApplyBlockOutcome(
    val needsVpnConsent: Intent?,
    val sweepResult: MemorySweepResult?
)
