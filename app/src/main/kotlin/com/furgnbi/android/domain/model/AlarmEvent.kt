package com.furgnbi.android.domain.model

data class AlarmEvent(
    val id: Long = 0,
    val deviceId: String,
    val alarmType: AlarmType,
    val severity: AlarmSeverity,
    val triggerValue: Float?,
    val triggeredAt: Long,
    val resolvedAt: Long? = null,
    val isPublished: Boolean = false
)
