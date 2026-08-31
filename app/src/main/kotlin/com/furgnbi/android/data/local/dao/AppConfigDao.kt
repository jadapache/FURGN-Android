package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AppConfigEntity>)

    @Query("SELECT * FROM app_config WHERE key = :key LIMIT 1")
    suspend fun get(key: String): AppConfigEntity?

    @Query("SELECT * FROM app_config")
    suspend fun getAll(): List<AppConfigEntity>

    @Query("SELECT * FROM app_config")
    fun observeAll(): Flow<List<AppConfigEntity>>
}
