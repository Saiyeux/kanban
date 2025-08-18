package com.example.kanban.data.entity

import java.time.LocalDateTime

data class Task(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val timeSpentHours: Double = 0.0, // hours spent on this task
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true
)

enum class TaskStatus(val displayName: String) {
    PENDING("待处理"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成")
}