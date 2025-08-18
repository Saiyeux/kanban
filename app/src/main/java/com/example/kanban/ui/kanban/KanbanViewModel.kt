package com.example.kanban.ui.kanban

import androidx.lifecycle.*
import com.example.kanban.data.SimpleRepository
import com.example.kanban.data.entity.Task
import com.example.kanban.data.entity.Event
import com.example.kanban.data.entity.TaskWithEvents
import com.example.kanban.data.entity.Priority
import com.example.kanban.data.entity.EventStatus
import java.time.LocalDateTime

class KanbanViewModel(private val repository: SimpleRepository) : ViewModel() {

    val allTasks = repository.tasks
    val allEvents = repository.events
    val tasksWithEvents = repository.tasksWithEvents
    val selectedTask = repository.selectedTask

    // UI state
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    init {
        _uiState.value = UiState()
    }

    fun createTask(title: String, description: String) {
        repository.addTask(title, description)
    }

    fun updateTask(task: Task) {
        repository.updateTask(task)
    }

    fun deleteTask(taskId: Long) {
        repository.deleteTask(taskId)
    }

    fun selectTask(taskId: Long) {
        repository.selectTask(taskId)
        _uiState.value = _uiState.value?.copy(selectedTaskId = taskId)
    }

    fun clearSelectedTask() {
        _uiState.value = _uiState.value?.copy(selectedTaskId = null)
    }

    fun createEvent(taskId: Long, title: String, description: String, priority: Priority, estimatedDuration: Int, dueDate: LocalDateTime?) {
        repository.addEvent(taskId, title, description, priority, estimatedDuration, dueDate)
    }

    fun updateEvent(event: Event) {
        repository.updateEvent(event)
    }

    fun deleteEvent(eventId: Long) {
        repository.deleteEvent(eventId)
    }

    fun updateEventStatus(eventId: Long, status: EventStatus) {
        repository.updateEventStatus(eventId, status)
    }

    fun moveEvent(eventId: Long, newPosition: Int) {
        repository.moveEvent(eventId, newPosition)
    }

    fun getEventsForTask(taskId: Long): List<Event> {
        return repository.getEventsForTask(taskId)
    }

    // UI helpers
    fun showCreateTaskDialog() {
        _uiState.value = _uiState.value?.copy(showCreateTaskDialog = true)
    }

    fun hideCreateTaskDialog() {
        _uiState.value = _uiState.value?.copy(showCreateTaskDialog = false)
    }

    fun showCreateEventDialog() {
        _uiState.value = _uiState.value?.copy(showCreateEventDialog = true)
    }

    fun hideCreateEventDialog() {
        _uiState.value = _uiState.value?.copy(showCreateEventDialog = false)
    }

    data class UiState(
        val selectedTaskId: Long? = null,
        val showCreateTaskDialog: Boolean = false,
        val showCreateEventDialog: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}

class KanbanViewModelFactory(private val repository: SimpleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KanbanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KanbanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}