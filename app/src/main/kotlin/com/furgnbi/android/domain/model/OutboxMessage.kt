package com.furgnbi.android.domain.model

data class OutboxMessage(
    val id: Long = 0,
    val topic: String,
    val payloadJson: String,
    val qos: Int = 1,
    val retryCount: Int = 0,
    val status: OutboxStatus = OutboxStatus.PENDING,
    val createdAt: Long,
    val lastAttemptAt: Long? = null
)
