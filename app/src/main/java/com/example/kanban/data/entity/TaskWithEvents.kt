package com.example.kanban.data.entity

data class TaskWithEvents(
    val task: Task,
    val events: List<Event> = emptyList()
) {
    val totalEstimatedDuration: Int
        get() = events.sumOf { it.estimatedDuration }
    
    val totalActualDuration: Int
        get() = events.sumOf { it.actualDuration }
    
    val completedEventsCount: Int
        get() = events.count { it.status == EventStatus.COMPLETED }
    
    val totalEventsCount: Int
        get() = events.size
    
    val progress: Float
        get() = if (totalEventsCount == 0) 0f else completedEventsCount.toFloat() / totalEventsCount
    
    val hasInProgressEvents: Boolean
        get() = events.any { it.status == EventStatus.IN_PROGRESS }
    
    val isCompleted: Boolean
        get() = totalEventsCount > 0 && completedEventsCount == totalEventsCount
}