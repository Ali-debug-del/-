package com.example.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.BlockScheduleEntity
import java.util.Calendar

class ScheduleAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(schedule: BlockScheduleEntity): Long {
        cancelAlarms(schedule.id)
        if (!schedule.isEnabled) return 0L

        val nextStart = computeNextOccurrenceMillis(
            hour = schedule.startHour,
            minute = schedule.startMinute,
            repeatDaily = schedule.repeatDaily,
            daysMask = schedule.daysMask
        )

        val startPi = buildPendingIntent(
            scheduleId = schedule.id,
            action = ScheduleReceiver.ACTION_START_SCHEDULE,
            requestCodeOffset = 10000
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextStart,
            startPi
        )

        if (schedule.hasEndTime) {
            val nextEnd = computeEndOccurrenceMillis(
                startMillis = nextStart,
                startHour = schedule.startHour,
                startMinute = schedule.startMinute,
                endHour = schedule.endHour,
                endMinute = schedule.endMinute
            )
            val endPi = buildPendingIntent(
                scheduleId = schedule.id,
                action = ScheduleReceiver.ACTION_END_SCHEDULE,
                requestCodeOffset = 20000
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextEnd,
                endPi
            )
        }

        return nextStart
    }

    fun cancelAlarms(scheduleId: Long) {
        val startPi = buildPendingIntent(
            scheduleId = scheduleId,
            action = ScheduleReceiver.ACTION_START_SCHEDULE,
            requestCodeOffset = 10000
        )
        val endPi = buildPendingIntent(
            scheduleId = scheduleId,
            action = ScheduleReceiver.ACTION_END_SCHEDULE,
            requestCodeOffset = 20000
        )
        alarmManager.cancel(startPi)
        alarmManager.cancel(endPi)
    }

    private fun buildPendingIntent(
        scheduleId: Long,
        action: String,
        requestCodeOffset: Int
    ): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            this.action = action
            putExtra(ScheduleReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val reqCode = (scheduleId.toInt() and 0xFFFF) + requestCodeOffset
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun computeNextOccurrenceMillis(
            hour: Int,
            minute: Int,
            repeatDaily: Boolean,
            daysMask: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (cal.timeInMillis <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (repeatDaily && daysMask != 0) {
                for (i in 0 until 7) {
                    val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0..6
                    if ((daysMask and (1 shl dayIndex)) != 0) {
                        break
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return cal.timeInMillis
        }

        fun computeEndOccurrenceMillis(
            startMillis: Long,
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int
        ): Long {
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startMillis
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute
            if (endTotalMinutes <= startTotalMinutes || endCal.timeInMillis <= startMillis) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return endCal.timeInMillis
        }
    }
}
