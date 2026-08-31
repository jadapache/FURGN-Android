package com.furgnbi.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox_queue")
data class OutboxQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "topic") val topic: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "qos") val qos: Int = 1,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "status") val status: String = "PENDING", // PENDING | SENT | FAILED
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_attempt_at") val lastAttemptAt: Long? = null
)
