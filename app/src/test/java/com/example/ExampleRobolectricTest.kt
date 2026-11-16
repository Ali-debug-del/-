package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppRuleEntity
import com.example.data.local.AppShieldDatabase
import com.example.data.local.BlockScheduleEntity
import com.example.data.local.MemoryTelemetryEntity
import com.example.data.model.BlockMode
import com.example.system.ScheduleAlarmManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("درع التطبيقات", appName)
    }

    @Test
    fun `room persists rules and schedules for memory internet or both`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.appShieldDao()

        dao.upsertRules(
            listOf(
                AppRuleEntity("com.social.app", "Social", BlockMode.INTERNET_ONLY, isActive = true),
                AppRuleEntity("com.game.app", "Game", BlockMode.MEMORY_ONLY, isActive = true),
                AppRuleEntity("com.heavy.app", "Heavy", BlockMode.BOTH, isActive = true)
            )
        )

        val rules = dao.observeAllRules().first()
        assertEquals(3, rules.size)
        assertTrue(rules.first { it.packageName == "com.heavy.app" }.blockMode.blocksInternet)
        assertTrue(rules.first { it.packageName == "com.heavy.app" }.blockMode.blocksMemory)

        val scheduleId = dao.insertSchedule(
            BlockScheduleEntity(
                title = "وقت العمل",
                packageNamesCsv = "com.social.app,com.game.app",
                appNamesPreview = "Social، Game",
                blockMode = BlockMode.BOTH,
                startHour = 9,
                startMinute = 0,
                endHour = 17,
                endMinute = 0,
                hasEndTime = true,
                repeatDaily = true
            )
        )
        val savedSchedule = dao.getScheduleByIdSync(scheduleId)
        assertEquals(listOf("com.social.app", "com.game.app"), savedSchedule?.targetPackages())
        db.close()
    }

    @Test
    fun `room records memory telemetry and sums freed ram`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.appShieldDao()

        dao.insertTelemetry(
            MemoryTelemetryEntity(
                timestamp = 1000L,
                totalRamMb = 4096L,
                usedRamMb = 2500L,
                availableRamMb = 1596L,
                blockedAppsRamMb = 240.5,
                blockedAppsCount = 3,
                freedDeltaMb = 0L,
                eventType = MemoryTelemetryEntity.EVENT_SAMPLE
            )
        )
        dao.insertTelemetry(
            MemoryTelemetryEntity(
                timestamp = 2000L,
                totalRamMb = 4096L,
                usedRamMb = 2320L,
                availableRamMb = 1776L,
                blockedAppsRamMb = 60.0,
                blockedAppsCount = 3,
                freedDeltaMb = 180L,
                eventType = MemoryTelemetryEntity.EVENT_SWEEP
            )
        )

        val points = dao.observeMemoryTelemetry().first()
        val totalFreed = dao.observeTotalFreedMb().first()
        assertEquals(2, points.size)
        assertEquals(180L, totalFreed)
        db.close()
    }

    @Test
    fun `schedule alarm time calculation handles overnight windows`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextStart = ScheduleAlarmManager.computeNextOccurrenceMillis(
            hour = 23,
            minute = 0,
            repeatDaily = true,
            daysMask = BlockScheduleEntity.ALL_DAYS_MASK,
            nowMillis = baseCal.timeInMillis
        )
        val endMillis = ScheduleAlarmManager.computeEndOccurrenceMillis(
            startMillis = nextStart,
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0
        )
        assertTrue(nextStart > baseCal.timeInMillis)
        assertEquals(8L * 60L * 60L * 1000L, endMillis - nextStart)
    }
}
