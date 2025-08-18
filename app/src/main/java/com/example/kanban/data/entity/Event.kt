package com.example.kanban.data.entity

import java.time.LocalDateTime

data class Event(
    val id: Long = 0,
    val taskId: Long = 0,
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: EventStatus = EventStatus.PENDING,
    val estimatedDuration: Int = 0, // minutes
    val actualDuration: Int = 0, // minutes
    val timeSpentHours: Double = 0.0, // hours spent on this event
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val position: Int = 0 // for ordering events within a task
)

enum class Priority(val displayName: String, val colorResource: String) {
    LOW("低", "#4CAF50"),
    MEDIUM("中", "#FF9800"), 
    HIGH("高", "#F44336"),
    URGENT("紧急", "#9C27B0")
}

enum class EventStatus(val displayName: String) {
    PENDING("待处理"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消")
}