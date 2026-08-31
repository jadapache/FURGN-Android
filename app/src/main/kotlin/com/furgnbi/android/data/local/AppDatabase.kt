package com.furgnbi.android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.furgnbi.android.data.local.dao.*
import com.furgnbi.android.data.local.entity.*

@Database(
    entities = [
        TelemetryRecordEntity::class,
        LocationRecordEntity::class,
        AlarmRecordEntity::class,
        OutboxQueueEntity::class,
        PlcParameterEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun telemetryDao(): TelemetryDao
    abstract fun locationDao(): LocationDao
    abstract fun alarmDao(): AlarmDao
    abstract fun outboxDao(): OutboxDao
    abstract fun plcParameterDao(): PlcParameterDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DATABASE_NAME = "furgn_bi.db"

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
