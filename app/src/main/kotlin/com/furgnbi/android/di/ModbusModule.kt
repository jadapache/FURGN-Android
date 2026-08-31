package com.furgnbi.android.di

import com.furgnbi.android.data.modbus.ModbusRepository
import com.furgnbi.android.data.modbus.ModbusRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModbusModule {

    @Binds
    @Singleton
    abstract fun bindModbusRepository(impl: ModbusRepositoryImpl): ModbusRepository
}
