package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppShieldDao {

    @Query("SELECT * FROM app_rules ORDER BY updatedAt DESC")
    fun observeAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllRulesSync(): List<AppRuleEntity>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleByPackageSync(packageName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<AppRuleEntity>)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM app_rules")
    suspend fun clearAllRules()

    @Query("UPDATE app_rules SET memoryKillsCount = memoryKillsCount + 1, updatedAt = :timestamp WHERE packageName IN (:packages)")
    suspend fun incrementMemoryKills(packages: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET blockedPacketsCount = blockedPacketsCount + :delta WHERE packageName IN (:packages)")
    suspend fun incrementBlockedPacketsForPackages(packages: List<String>, delta: Long)

    // Schedules
    @Query("SELECT * FROM block_schedules ORDER BY createdAt DESC")
    fun observeAllSchedules(): Flow<List<BlockScheduleEntity>>

    @Query("SELECT * FROM block_schedules WHERE isEnabled = 1")
    suspend fun getEnabledSchedulesSync(): List<BlockScheduleEntity>

    @Query("SELECT * FROM block_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleByIdSync(id: Long): BlockScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BlockScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: BlockScheduleEntity)

    @Query("DELETE FROM block_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    // Logs
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC LIMIT 120")
    fun observeRecentLogs(): Flow<List<BlockLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BlockLogEntity)

    @Query("DELETE FROM block_logs")
    suspend fun clearLogs()

    // Memory Telemetry
    @Query("SELECT * FROM memory_telemetry ORDER BY timestamp ASC LIMIT 60")
    fun observeMemoryTelemetry(): Flow<List<MemoryTelemetryEntity>>

    @Query("SELECT COALESCE(SUM(freedDeltaMb), 0) FROM memory_telemetry")
    fun observeTotalFreedMb(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(point: MemoryTelemetryEntity)

    @Query("DELETE FROM memory_telemetry WHERE id NOT IN (SELECT id FROM memory_telemetry ORDER BY timestamp DESC LIMIT 60)")
    suspend fun pruneOldTelemetry()
}
