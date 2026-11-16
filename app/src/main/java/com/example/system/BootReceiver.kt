package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppShieldDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppShieldDatabase.getInstance(context.applicationContext).appShieldDao()
                val alarmMgr = ScheduleAlarmManager(context.applicationContext)
                val enabledSchedules = dao.getEnabledSchedulesSync()
                for (schedule in enabledSchedules) {
                    val nextStart = alarmMgr.scheduleAlarms(schedule)
                    if (nextStart != schedule.nextStartMillis) {
                        dao.updateSchedule(schedule.copy(nextStartMillis = nextStart))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
