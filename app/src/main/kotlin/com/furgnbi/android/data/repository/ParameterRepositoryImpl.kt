package com.furgnbi.android.data.repository

import com.furgnbi.android.data.local.dao.PlcParameterDao
import com.furgnbi.android.domain.model.PlcParameter
import com.furgnbi.android.domain.model.toDomain
import com.furgnbi.android.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ParameterRepository {
    suspend fun getAllParameters(): List<PlcParameter>
    fun observeParameters(): Flow<List<PlcParameter>>
    suspend fun getParameter(id: Int): PlcParameter?
    suspend fun updateParameter(parameter: PlcParameter)
    suspend fun updateValue(id: Int, value: Double)
    suspend fun insertAll(parameters: List<PlcParameter>)
    suspend fun count(): Int
}

@Singleton
class ParameterRepositoryImpl @Inject constructor(
    private val plcParameterDao: PlcParameterDao
) : ParameterRepository {

    override suspend fun getAllParameters(): List<PlcParameter> =
        plcParameterDao.getAll().map { it.toDomain() }

    override fun observeParameters(): Flow<List<PlcParameter>> =
        plcParameterDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getParameter(id: Int): PlcParameter? =
        plcParameterDao.getById(id)?.toDomain()

    override suspend fun updateParameter(parameter: PlcParameter) =
        plcParameterDao.insert(parameter.toEntity())

    override suspend fun updateValue(id: Int, value: Double) =
        plcParameterDao.updateValue(id, value)

    override suspend fun insertAll(parameters: List<PlcParameter>) =
        plcParameterDao.insertAll(parameters.map { it.toEntity() })

    override suspend fun count(): Int = plcParameterDao.count()
}
