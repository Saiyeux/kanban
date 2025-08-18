package com.example.kanban.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.kanban.data.entity.Task
import com.example.kanban.data.entity.Event
import com.example.kanban.data.entity.TaskWithEvents
import com.example.kanban.data.entity.Priority
import com.example.kanban.data.entity.EventStatus
import com.example.kanban.data.entity.TaskStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SimpleRepository(private val context: Context? = null) {
    
    companion object {
        private const val PREFS_NAME = "kanban_data"
        private const val KEY_TASKS = "tasks"
        private const val KEY_EVENTS = "events"
        private const val KEY_NEXT_TASK_ID = "next_task_id"
        private const val KEY_NEXT_EVENT_ID = "next_event_id"
        private const val KEY_DATA_VERSION = "data_version"
        private const val CURRENT_DATA_VERSION = 1
    }
    
    private val sharedPrefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks
    
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events
    
    private val _tasksWithEvents = MutableLiveData<List<TaskWithEvents>>()
    val tasksWithEvents: LiveData<List<TaskWithEvents>> = _tasksWithEvents
    
    private val _selectedTask = MutableLiveData<TaskWithEvents?>()
    val selectedTask: LiveData<TaskWithEvents?> = _selectedTask
    
    private var nextTaskId: Long
    private var nextEventId: Long
    private val taskList = mutableListOf<Task>()
    private val eventList = mutableListOf<Event>()
    
    init {
        // 从SharedPreferences加载数据
        nextTaskId = sharedPrefs?.getLong(KEY_NEXT_TASK_ID, 1L) ?: 1L
        nextEventId = sharedPrefs?.getLong(KEY_NEXT_EVENT_ID, 1L) ?: 1L
        
        // 检查数据版本并进行迁移
        val savedVersion = sharedPrefs?.getInt(KEY_DATA_VERSION, 0) ?: 0
        if (savedVersion < CURRENT_DATA_VERSION) {
            performDataMigration(savedVersion)
        }
        
        loadTasksFromStorage()
        loadEventsFromStorage()
        
        // 如果没有数据，添加示例数据
        if (taskList.isEmpty()) {
            addSampleData()
        } else {
            updateLiveData()
        }
        
        _selectedTask.value = null
    }
    
    private fun performDataMigration(fromVersion: Int) {
        // 未来版本的数据迁移逻辑
        when (fromVersion) {
            0 -> {
                // 从版本0（无版本）迁移到版本1
                // 当前无需特殊处理，只是标记版本
            }
            // 未来可以添加更多版本迁移逻辑
        }
        
        // 更新数据版本
        sharedPrefs?.edit()?.putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)?.apply()
    }
    
    private fun addSampleData() {
        // 创建示例任务
        val task1 = Task(id = nextTaskId++, title = "开发用户登录功能", description = "实现用户登录、注册和密码重置功能", status = TaskStatus.PENDING)
        val task2 = Task(id = nextTaskId++, title = "设计主界面UI", description = "设计应用主界面和导航结构", status = TaskStatus.PENDING)
        
        taskList.addAll(listOf(task1, task2))
        
        // 为第一个任务添加事件
        val events1 = listOf(
            Event(id = nextEventId++, taskId = task1.id, title = "设计数据库结构", description = "设计用户表和相关字段", priority = Priority.HIGH, status = EventStatus.COMPLETED, estimatedDuration = 120, actualDuration = 90, timeSpentHours = 1.5, position = 0),
            Event(id = nextEventId++, taskId = task1.id, title = "实现注册API", description = "创建用户注册接口", priority = Priority.MEDIUM, status = EventStatus.IN_PROGRESS, estimatedDuration = 180, actualDuration = 60, timeSpentHours = 1.0, position = 1),
            Event(id = nextEventId++, taskId = task1.id, title = "前端注册页面", description = "设计和实现注册页面", priority = Priority.MEDIUM, status = EventStatus.PENDING, estimatedDuration = 240, timeSpentHours = 0.0, position = 2)
        )
        
        // 为第二个任务添加事件
        val events2 = listOf(
            Event(id = nextEventId++, taskId = task2.id, title = "线框图设计", description = "绘制主要页面的线框图", priority = Priority.LOW, status = EventStatus.PENDING, estimatedDuration = 300, timeSpentHours = 0.0, position = 0)
        )
        
        eventList.addAll(events1 + events2)
        
        updateLiveData()
        saveDataToStorage()
    }
    
    private fun loadTasksFromStorage() {
        val tasksJson = sharedPrefs?.getString(KEY_TASKS, "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(tasksJson)
            taskList.clear()
            for (i in 0 until jsonArray.length()) {
                val taskJson = jsonArray.getJSONObject(i)
                val task = Task(
                    id = taskJson.getLong("id"),
                    title = taskJson.getString("title"),
                    description = taskJson.optString("description", ""),
                    status = TaskStatus.valueOf(taskJson.optString("status", "PENDING")),
                    timeSpentHours = taskJson.optDouble("timeSpentHours", 0.0),
                    createdAt = LocalDateTime.parse(taskJson.getString("createdAt"), dateFormatter),
                    updatedAt = LocalDateTime.parse(taskJson.getString("updatedAt"), dateFormatter),
                    isActive = taskJson.optBoolean("isActive", true)
                )
                taskList.add(task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadEventsFromStorage() {
        val eventsJson = sharedPrefs?.getString(KEY_EVENTS, "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(eventsJson)
            eventList.clear()
            for (i in 0 until jsonArray.length()) {
                val eventJson = jsonArray.getJSONObject(i)
                val event = Event(
                    id = eventJson.getLong("id"),
                    taskId = eventJson.getLong("taskId"),
                    title = eventJson.getString("title"),
                    description = eventJson.optString("description", ""),
                    priority = Priority.valueOf(eventJson.optString("priority", "MEDIUM")),
                    status = EventStatus.valueOf(eventJson.optString("status", "PENDING")),
                    estimatedDuration = eventJson.optInt("estimatedDuration", 0),
                    actualDuration = eventJson.optInt("actualDuration", 0),
                    timeSpentHours = eventJson.optDouble("timeSpentHours", 0.0),
                    createdAt = LocalDateTime.parse(eventJson.getString("createdAt"), dateFormatter),
                    updatedAt = LocalDateTime.parse(eventJson.getString("updatedAt"), dateFormatter),
                    dueDate = if (eventJson.has("dueDate") && !eventJson.isNull("dueDate")) {
                        LocalDateTime.parse(eventJson.getString("dueDate"), dateFormatter)
                    } else null,
                    position = eventJson.optInt("position", 0)
                )
                eventList.add(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveDataToStorage() {
        if (sharedPrefs == null) return
        
        val editor = sharedPrefs.edit()
        
        // 保存任务
        val tasksJson = JSONArray()
        for (task in taskList) {
            val taskJson = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("status", task.status.name)
                put("timeSpentHours", task.timeSpentHours)
                put("createdAt", task.createdAt.format(dateFormatter))
                put("updatedAt", task.updatedAt.format(dateFormatter))
                put("isActive", task.isActive)
            }
            tasksJson.put(taskJson)
        }
        editor.putString(KEY_TASKS, tasksJson.toString())
        
        // 保存事件
        val eventsJson = JSONArray()
        for (event in eventList) {
            val eventJson = JSONObject().apply {
                put("id", event.id)
                put("taskId", event.taskId)
                put("title", event.title)
                put("description", event.description)
                put("priority", event.priority.name)
                put("status", event.status.name)
                put("estimatedDuration", event.estimatedDuration)
                put("actualDuration", event.actualDuration)
                put("timeSpentHours", event.timeSpentHours)
                put("createdAt", event.createdAt.format(dateFormatter))
                put("updatedAt", event.updatedAt.format(dateFormatter))
                if (event.dueDate != null) {
                    put("dueDate", event.dueDate.format(dateFormatter))
                }
                put("position", event.position)
            }
            eventsJson.put(eventJson)
        }
        editor.putString(KEY_EVENTS, eventsJson.toString())
        
        // 保存ID计数器
        editor.putLong(KEY_NEXT_TASK_ID, nextTaskId)
        editor.putLong(KEY_NEXT_EVENT_ID, nextEventId)
        
        editor.apply()
    }
    
    private fun updateLiveData() {
        // 更新任务状态和用时
        updateTaskStatusAndTime()
        
        _tasks.value = taskList.toList()
        _events.value = eventList.toList()
        
        val tasksWithEventsData = taskList.map { task ->
            val taskEvents = eventList.filter { it.taskId == task.id }.sortedBy { it.position }
            TaskWithEvents(task, taskEvents)
        }
        _tasksWithEvents.value = tasksWithEventsData
    }
    
    private fun updateTaskStatusAndTime() {
        for (i in taskList.indices) {
            val task = taskList[i]
            val taskEvents = eventList.filter { it.taskId == task.id }
            
            if (taskEvents.isEmpty()) {
                // 无事件的任务保持待处理状态
                taskList[i] = task.copy(
                    status = TaskStatus.PENDING,
                    timeSpentHours = 0.0
                )
            } else {
                // 计算任务状态
                val newStatus = when {
                    taskEvents.all { it.status == EventStatus.COMPLETED } -> TaskStatus.COMPLETED
                    taskEvents.any { it.status == EventStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
                    taskEvents.any { it.status == EventStatus.COMPLETED } -> TaskStatus.IN_PROGRESS
                    else -> TaskStatus.PENDING
                }
                
                // 计算任务总用时
                val totalTimeSpent = taskEvents.sumOf { it.timeSpentHours }
                
                taskList[i] = task.copy(
                    status = newStatus,
                    timeSpentHours = totalTimeSpent
                )
            }
        }
    }
    
    fun addTask(title: String, description: String) {
        val newTask = Task(
            id = nextTaskId++,
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            timeSpentHours = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        taskList.add(newTask)
        updateLiveData()
        saveDataToStorage()
    }
    
    fun updateTask(task: Task) {
        val index = taskList.indexOfFirst { it.id == task.id }
        if (index != -1) {
            taskList[index] = task.copy(updatedAt = LocalDateTime.now())
            updateLiveData()
            saveDataToStorage()
            
            // 更新选中任务
            if (_selectedTask.value?.task?.id == task.id) {
                selectTask(task.id)
            }
        }
    }
    
    fun deleteTask(taskId: Long) {
        taskList.removeAll { it.id == taskId }
        eventList.removeAll { it.taskId == taskId }
        updateLiveData()
        saveDataToStorage()
        
        // 如果删除的是选中的任务，清除选中状态
        if (_selectedTask.value?.task?.id == taskId) {
            _selectedTask.value = null
        }
    }
    
    fun selectTask(taskId: Long) {
        val task = taskList.find { it.id == taskId }
        if (task != null) {
            val events = eventList.filter { it.taskId == taskId }.sortedBy { it.position }
            _selectedTask.value = TaskWithEvents(task, events)
        }
    }
    
    fun addEvent(taskId: Long, title: String, description: String, priority: Priority, estimatedDuration: Int, dueDate: LocalDateTime?) {
        val maxPosition = eventList.filter { it.taskId == taskId }.maxOfOrNull { it.position } ?: -1
        val newEvent = Event(
            id = nextEventId++,
            taskId = taskId,
            title = title,
            description = description,
            priority = priority,
            status = EventStatus.PENDING,
            estimatedDuration = estimatedDuration,
            timeSpentHours = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            dueDate = dueDate,
            position = maxPosition + 1
        )
        eventList.add(newEvent)
        updateLiveData()
        saveDataToStorage()
        
        // 更新选中任务
        if (_selectedTask.value?.task?.id == taskId) {
            selectTask(taskId)
        }
    }
    
    fun updateEvent(event: Event) {
        val index = eventList.indexOfFirst { it.id == event.id }
        if (index != -1) {
            eventList[index] = event.copy(updatedAt = LocalDateTime.now())
            updateLiveData()
            saveDataToStorage()
            
            // 更新选中任务
            if (_selectedTask.value?.task?.id == event.taskId) {
                selectTask(event.taskId)
            }
        }
    }
    
    fun deleteEvent(eventId: Long) {
        val event = eventList.find { it.id == eventId }
        eventList.removeAll { it.id == eventId }
        updateLiveData()
        saveDataToStorage()
        
        // 更新选中任务
        event?.let { 
            if (_selectedTask.value?.task?.id == it.taskId) {
                selectTask(it.taskId)
            }
        }
    }
    
    fun updateEventStatus(eventId: Long, status: EventStatus) {
        val index = eventList.indexOfFirst { it.id == eventId }
        if (index != -1) {
            eventList[index] = eventList[index].copy(
                status = status,
                updatedAt = LocalDateTime.now()
            )
            updateLiveData()
            saveDataToStorage()
            
            // 更新选中任务
            val event = eventList[index]
            if (_selectedTask.value?.task?.id == event.taskId) {
                selectTask(event.taskId)
            }
        }
    }
    
    fun moveEvent(eventId: Long, newPosition: Int) {
        val event = eventList.find { it.id == eventId } ?: return
        val taskEvents = eventList.filter { it.taskId == event.taskId && it.id != eventId }.sortedBy { it.position }
        
        // 重新排列事件位置
        val updatedEvents = mutableListOf<Event>()
        var insertIndex = 0
        
        for (i in taskEvents.indices) {
            if (i == newPosition) {
                updatedEvents.add(event.copy(position = insertIndex++, updatedAt = LocalDateTime.now()))
            }
            updatedEvents.add(taskEvents[i].copy(position = insertIndex++, updatedAt = LocalDateTime.now()))
        }
        
        if (newPosition >= taskEvents.size) {
            updatedEvents.add(event.copy(position = insertIndex, updatedAt = LocalDateTime.now()))
        }
        
        // 更新事件列表
        eventList.removeAll { it.taskId == event.taskId }
        eventList.addAll(updatedEvents)
        updateLiveData()
        saveDataToStorage()
        
        // 更新选中任务
        if (_selectedTask.value?.task?.id == event.taskId) {
            selectTask(event.taskId)
        }
    }
    
    fun getEventsForTask(taskId: Long): List<Event> {
        return eventList.filter { it.taskId == taskId }.sortedBy { it.position }
    }
    
    // 数据导出功能（用于备份）
    fun exportData(): String {
        val exportData = JSONObject().apply {
            put("version", CURRENT_DATA_VERSION)
            put("exportDate", LocalDateTime.now().format(dateFormatter))
            put("nextTaskId", nextTaskId)
            put("nextEventId", nextEventId)
            
            // 导出任务
            val tasksJson = JSONArray()
            for (task in taskList) {
                val taskJson = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("status", task.status.name)
                    put("timeSpentHours", task.timeSpentHours)
                    put("createdAt", task.createdAt.format(dateFormatter))
                    put("updatedAt", task.updatedAt.format(dateFormatter))
                    put("isActive", task.isActive)
                }
                tasksJson.put(taskJson)
            }
            put("tasks", tasksJson)
            
            // 导出事件
            val eventsJson = JSONArray()
            for (event in eventList) {
                val eventJson = JSONObject().apply {
                    put("id", event.id)
                    put("taskId", event.taskId)
                    put("title", event.title)
                    put("description", event.description)
                    put("priority", event.priority.name)
                    put("status", event.status.name)
                    put("estimatedDuration", event.estimatedDuration)
                    put("actualDuration", event.actualDuration)
                    put("timeSpentHours", event.timeSpentHours)
                    put("createdAt", event.createdAt.format(dateFormatter))
                    put("updatedAt", event.updatedAt.format(dateFormatter))
                    if (event.dueDate != null) {
                        put("dueDate", event.dueDate.format(dateFormatter))
                    }
                    put("position", event.position)
                }
                eventsJson.put(eventJson)
            }
            put("events", eventsJson)
        }
        return exportData.toString(2) // 格式化输出
    }
    
    // 数据导入功能（用于恢复备份）
    fun importData(jsonString: String): Boolean {
        try {
            val importData = JSONObject(jsonString)
            val version = importData.optInt("version", 1)
            
            // 清除现有数据
            taskList.clear()
            eventList.clear()
            
            // 恢复ID计数器
            nextTaskId = importData.optLong("nextTaskId", 1L)
            nextEventId = importData.optLong("nextEventId", 1L)
            
            // 导入任务
            val tasksJson = importData.optJSONArray("tasks")
            if (tasksJson != null) {
                for (i in 0 until tasksJson.length()) {
                    val taskJson = tasksJson.getJSONObject(i)
                    val task = Task(
                        id = taskJson.getLong("id"),
                        title = taskJson.getString("title"),
                        description = taskJson.optString("description", ""),
                        status = TaskStatus.valueOf(taskJson.optString("status", "PENDING")),
                        timeSpentHours = taskJson.optDouble("timeSpentHours", 0.0),
                        createdAt = LocalDateTime.parse(taskJson.getString("createdAt"), dateFormatter),
                        updatedAt = LocalDateTime.parse(taskJson.getString("updatedAt"), dateFormatter),
                        isActive = taskJson.optBoolean("isActive", true)
                    )
                    taskList.add(task)
                }
            }
            
            // 导入事件
            val eventsJson = importData.optJSONArray("events")
            if (eventsJson != null) {
                for (i in 0 until eventsJson.length()) {
                    val eventJson = eventsJson.getJSONObject(i)
                    val event = Event(
                        id = eventJson.getLong("id"),
                        taskId = eventJson.getLong("taskId"),
                        title = eventJson.getString("title"),
                        description = eventJson.optString("description", ""),
                        priority = Priority.valueOf(eventJson.optString("priority", "MEDIUM")),
                        status = EventStatus.valueOf(eventJson.optString("status", "PENDING")),
                        estimatedDuration = eventJson.optInt("estimatedDuration", 0),
                        actualDuration = eventJson.optInt("actualDuration", 0),
                        timeSpentHours = eventJson.optDouble("timeSpentHours", 0.0),
                        createdAt = LocalDateTime.parse(eventJson.getString("createdAt"), dateFormatter),
                        updatedAt = LocalDateTime.parse(eventJson.getString("updatedAt"), dateFormatter),
                        dueDate = if (eventJson.has("dueDate") && !eventJson.isNull("dueDate")) {
                            LocalDateTime.parse(eventJson.getString("dueDate"), dateFormatter)
                        } else null,
                        position = eventJson.optInt("position", 0)
                    )
                    eventList.add(event)
                }
            }
            
            // 保存导入的数据并更新UI
            updateLiveData()
            saveDataToStorage()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}