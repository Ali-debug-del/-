package com.example.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppRuleEntity
import com.example.data.local.AppShieldDatabase
import com.example.data.local.BlockLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppShieldDatabase.getInstance(context.applicationContext)
                val dao = db.appShieldDao()
                val schedule = dao.getScheduleByIdSync(scheduleId) ?: return@launch
                if (!schedule.isEnabled) return@launch

                val packages = schedule.targetPackages()
                val pm = context.packageManager

                when (intent.action) {
                    ACTION_START_SCHEDULE -> {
                        val rules = packages.map { pkg ->
                            val label = runCatching {
                                val ai = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(ai).toString()
                            }.getOrDefault(pkg)
                            val existing = dao.getRuleByPackageSync(pkg)
                            AppRuleEntity(
                                packageName = pkg,
                                appName = label,
                                blockMode = schedule.blockMode,
                                isActive = true,
                                updatedAt = System.currentTimeMillis(),
                                blockedPacketsCount = existing?.blockedPacketsCount ?: 0L,
                                memoryKillsCount = existing?.memoryKillsCount ?: 0
                            )
                        }
                        dao.upsertRules(rules)

                        if (schedule.blockMode.blocksMemory) {
                            val sweep = MemoryGuardManager(context.applicationContext)
                                .enforceMemoryBlock(packages)
                            dao.incrementMemoryKills(packages)
                            dao.insertLog(
                                BlockLogEntity(
                                    packageName = packages.firstOrNull() ?: "",
                                    appName = schedule.title,
                                    actionType = BlockLogEntity.ACTION_MEMORY_FREED,
                                    details = "تفعيل جدولة الذاكرة: استهداف ${sweep.targetedCount} تطبيق، تحرير ${sweep.freedMb} MB"
                                )
                            )
                        }

                        if (schedule.blockMode.blocksInternet &&
                            VpnService.prepare(context.applicationContext) == null
                        ) {
                            FirewallVpnService.startOrUpdate(context.applicationContext)
                        }

                        dao.insertLog(
                            BlockLogEntity(
                                packageName = packages.firstOrNull() ?: "",
                                appName = schedule.title,
                                actionType = BlockLogEntity.ACTION_SCHEDULE_STARTED,
                                details = "بدأت الجدولة (${schedule.blockMode.displayLabelAr()}) لـ ${packages.size} تطبيق"
                            )
                        )

                        val nextStart = if (schedule.repeatDaily) {
                            ScheduleAlarmManager(context.applicationContext)
                                .scheduleAlarms(schedule.copy(isCurrentlyTriggered = true))
                        } else {
                            schedule.nextStartMillis
                        }
                        dao.updateSchedule(
                            schedule.copy(
                                isCurrentlyTriggered = true,
                                nextStartMillis = nextStart
                            )
                        )

                        postScheduleNotification(
                            context,
                            scheduleId.toInt(),
                            "بدء الحجب المجدول: ${schedule.title}",
                            "تم تفعيل ${schedule.blockMode.displayLabelAr()} لـ ${packages.size} تطبيق"
                        )
                    }

                    ACTION_END_SCHEDULE -> {
                        for (pkg in packages) {
                            dao.deleteRule(pkg)
                        }

                        if (VpnService.prepare(context.applicationContext) == null) {
                            val remaining = dao.getAllRulesSync().filter { it.isActive }
                            if (remaining.any { it.blockMode.blocksInternet || it.blockMode.blocksMemory }) {
                                FirewallVpnService.startOrUpdate(context.applicationContext)
                            } else {
                                FirewallVpnService.stop(context.applicationContext)
                            }
                        }

                        dao.insertLog(
                            BlockLogEntity(
                                packageName = packages.firstOrNull() ?: "",
                                appName = schedule.title,
                                actionType = BlockLogEntity.ACTION_SCHEDULE_ENDED,
                                details = "انتهت الجدولة وتم فك الحجب عن ${packages.size} تطبيق"
                            )
                        )

                        dao.updateSchedule(
                            schedule.copy(
                                isCurrentlyTriggered = false,
                                isEnabled = schedule.repeatDaily
                            )
                        )

                        postScheduleNotification(
                            context,
                            scheduleId.toInt(),
                            "انتهاء الحجب المجدول: ${schedule.title}",
                            "تم رفع القيود المجدولة عن ${packages.size} تطبيق"
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postScheduleNotification(
        context: Context,
        id: Int,
        title: String,
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.schedule_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(5000 + id, notif)
    }

    companion object {
        const val ACTION_START_SCHEDULE = "com.example.action.START_SCHEDULE"
        const val ACTION_END_SCHEDULE = "com.example.action.END_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        private const val CHANNEL_ID = "appshield_schedule_channel"
    }
}
