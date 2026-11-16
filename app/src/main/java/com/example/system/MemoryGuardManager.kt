package com.example.system

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.model.MemorySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class MemorySweepResult(
    val targetedCount: Int,
    val freedMb: Long,
    val snapshotAfter: MemorySnapshot
)

class MemoryGuardManager(private val context: Context) {

    fun getMemorySnapshot(lastFreedMb: Long = FirewallStateHolder.state.value.lastFreedMb): MemorySnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val percent = (usedMb.toFloat() / totalMb.toFloat()).coerceIn(0f, 1f)

        val runningCount = runCatching {
            am.runningAppProcesses?.size ?: 0
        }.getOrDefault(0)

        return MemorySnapshot(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            usedRamMb = usedMb,
            usagePercent = percent,
            isLowMemory = memInfo.lowMemory,
            runningBackgroundAppsCount = runningCount,
            lastFreedMb = lastFreedMb
        )
    }

    suspend fun enforceMemoryBlock(targetPackages: List<String>): MemorySweepResult =
        withContext(Dispatchers.IO) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val before = getMemorySnapshot()

            val foregroundPkg = getForegroundPackageIfPermitted()
            var targeted = 0
            for (pkg in targetPackages.distinct()) {
                if (pkg == context.packageName) continue
                if (pkg == foregroundPkg) continue
                runCatching {
                    am.killBackgroundProcesses(pkg)
                    targeted++
                }
            }

            // Short settle delay to allow kernel memory reclamation before re-sampling
            delay(250L)
            val after = getMemorySnapshot()
            val freed = (after.availableRamMb - before.availableRamMb).coerceAtLeast(0L)
            FirewallStateHolder.recordMemorySweep(freed)
            MemorySweepResult(
                targetedCount = targeted,
                freedMb = freed,
                snapshotAfter = after.copy(lastFreedMb = freed)
            )
        }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getForegroundPackageIfPermitted(): String? {
        if (!hasUsageStatsPermission()) return null
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        }.getOrNull()
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
