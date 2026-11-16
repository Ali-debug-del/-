package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppRuleEntity::class,
        BlockScheduleEntity::class,
        BlockLogEntity::class,
        MemoryTelemetryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppShieldDatabase : RoomDatabase() {
    abstract fun appShieldDao(): AppShieldDao

    companion object {
        @Volatile
        private var INSTANCE: AppShieldDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS memory_telemetry (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        totalRamMb INTEGER NOT NULL,
                        usedRamMb INTEGER NOT NULL,
                        availableRamMb INTEGER NOT NULL,
                        blockedAppsRamMb REAL NOT NULL,
                        blockedAppsCount INTEGER NOT NULL,
                        freedDeltaMb INTEGER NOT NULL,
                        eventType TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppShieldDatabase::class.java,
                    "app_shield_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
