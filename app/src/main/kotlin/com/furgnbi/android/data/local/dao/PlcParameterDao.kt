package com.furgnbi.android.data.local.dao

import androidx.room.*
import com.furgnbi.android.data.local.entity.PlcParameterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlcParameterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PlcParameterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlcParameterEntity)

    @Query("SELECT * FROM plc_parameters ORDER BY id ASC")
    suspend fun getAll(): List<PlcParameterEntity>

    @Query("SELECT * FROM plc_parameters ORDER BY id ASC")
    fun observeAll(): Flow<List<PlcParameterEntity>>

    @Query("SELECT * FROM plc_parameters WHERE id = :id")
    suspend fun getById(id: Int): PlcParameterEntity?

    @Query("UPDATE plc_parameters SET current_value = :value, last_read_at = :now WHERE id = :id")
    suspend fun updateValue(id: Int, value: Double, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM plc_parameters")
    suspend fun count(): Int
}
