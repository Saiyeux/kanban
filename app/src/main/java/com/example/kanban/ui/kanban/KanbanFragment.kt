package com.example.kanban.ui.kanban

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kanban.R
import com.example.kanban.KanbanApplication
import com.example.kanban.data.SimpleRepository
import com.example.kanban.data.entity.Priority
import com.example.kanban.data.entity.EventStatus
import com.example.kanban.data.entity.Event
import com.example.kanban.data.entity.Task
import com.example.kanban.data.entity.TaskWithEvents
import com.example.kanban.data.entity.TaskStatus
import com.example.kanban.ui.adapter.TaskAdapter
import com.example.kanban.ui.adapter.EventAdapter
import com.example.kanban.ui.helper.EventItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.*
import kotlin.math.abs

class KanbanFragment : Fragment() {

    private lateinit var repository: SimpleRepository
    private lateinit var viewModel: KanbanViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var eventAdapter: EventAdapter

    // UI elements
    private lateinit var tasksRecycler: RecyclerView
    private lateinit var eventsRecycler: RecyclerView
    private lateinit var selectedTaskTitle: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var fabAddEvent: FloatingActionButton
    private lateinit var sidebarContainer: View
    private lateinit var sidebarOverlay: View
    private lateinit var sidebarToggle: ImageButton
    private lateinit var mainContainer: FrameLayout
    private lateinit var eventsStatsContainer: View
    private lateinit var totalEventsText: TextView
    private lateinit var completedEventsText: TextView
    private lateinit var inProgressEventsText: TextView
    private lateinit var pendingEventsText: TextView
    private lateinit var totalStatsContainer: View
    private lateinit var completedStatsContainer: View
    private lateinit var inProgressStatsContainer: View
    private lateinit var pendingStatsContainer: View
    
    // Event filtering
    private var currentFilter: EventStatus? = null
    
    // 手势和动画
    private lateinit var gestureDetector: GestureDetectorCompat
    private var isSidebarVisible = false
    private var sidebarWidth = 0
    private var currentAnimator: ValueAnimator? = null
    
    // 删除区域
    private lateinit var deleteZone: View
    private var isDeleteZoneVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kanban_simple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val application = requireActivity().application as KanbanApplication
        repository = application.repository
        viewModel = KanbanViewModelFactory(repository).create(KanbanViewModel::class.java)
        
        initViews()
        setupRecyclerViews()
        setupClickListeners()
        observeData()
    }

    private fun initViews() {
        tasksRecycler = view?.findViewById(R.id.tasks_recycler) ?: return
        eventsRecycler = view?.findViewById(R.id.events_recycler) ?: return
        selectedTaskTitle = view?.findViewById(R.id.selected_task_title) ?: return
        emptyStateContainer = view?.findViewById(R.id.empty_state_container) ?: return
        fabAddTask = view?.findViewById(R.id.fab_add_task) ?: return
        fabAddEvent = view?.findViewById(R.id.fab_add_event) ?: return
        sidebarContainer = view?.findViewById(R.id.sidebar_container) ?: return
        sidebarOverlay = view?.findViewById(R.id.sidebar_overlay) ?: return
        sidebarToggle = view?.findViewById(R.id.sidebar_toggle) ?: return
        mainContainer = view?.findViewById(R.id.main_container) ?: return
        eventsStatsContainer = view?.findViewById(R.id.events_stats_container) ?: return
        totalEventsText = view?.findViewById(R.id.total_events_text) ?: return
        completedEventsText = view?.findViewById(R.id.completed_events_text) ?: return
        inProgressEventsText = view?.findViewById(R.id.in_progress_events_text) ?: return
        pendingEventsText = view?.findViewById(R.id.pending_events_text) ?: return
        totalStatsContainer = view?.findViewById(R.id.total_stats_container) ?: return
        completedStatsContainer = view?.findViewById(R.id.completed_stats_container) ?: return
        inProgressStatsContainer = view?.findViewById(R.id.in_progress_stats_container) ?: return
        pendingStatsContainer = view?.findViewById(R.id.pending_stats_container) ?: return
        
        // 计算侧边栏宽度（280dp转像素）
        val density = resources.displayMetrics.density
        sidebarWidth = (280 * density).toInt()
        
        // 初始化删除区域
        initDeleteZone()
        
        setupGestureDetection()
    }

    private fun initDeleteZone() {
        // 创建删除区域视图
        deleteZone = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.RED)
            alpha = 0.8f
            visibility = View.GONE
            
            // 添加删除区域的提示文本
            val textView = TextView(context).apply {
                text = "拖拽到此处删除"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            // 将文本视图添加到删除区域
            addView(textView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        
        // 将删除区域添加到主容器
        mainContainer.addView(deleteZone, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            resources.getDimension(R.dimen.delete_zone_height).toInt()
        ).apply {
            gravity = android.view.Gravity.BOTTOM
        })
    }

    private fun setupGestureDetection() {
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // 检查是否是水平滑动并且水平距离大于垂直距离
                if (abs(diffX) > abs(diffY) && abs(diffX) > 120 && abs(velocityX) > 200) {
                    if (diffX > 0) {
                        // 向右滑动 - 显示侧边栏
                        if (!isSidebarVisible && e1.x < 80) { // 从左边缘开始滑动
                            showSidebar()
                            return true
                        }
                    } else {
                        // 向左滑动 - 隐藏侧边栏
                        if (isSidebarVisible) {
                            hideSidebar()
                            return true
                        }
                    }
                }
                return false
            }
        })
        
        mainContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupRecyclerViews() {
        // 任务适配器（侧边栏）
        taskAdapter = TaskAdapter(
            onTaskClick = { taskWithEvents ->
                viewModel.selectTask(taskWithEvents.task.id)
            },
            onTaskLongClick = { taskWithEvents ->
                showTaskContextMenu(taskWithEvents)
            }
        )
        tasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        tasksRecycler.adapter = taskAdapter

        // 事件适配器（主内容区域）
        eventAdapter = EventAdapter(
            onEventClick = { event ->
                // 点击事件会在适配器内部处理展开/收起
            },
            onEventEdit = { event ->
                showEditEventDialog(event)
            },
            onEventDelete = { event ->
                showDeleteEventDialog(event)
            },
            onEventLongClick = { event ->
                // 长按事件显示删除区域
                showDeleteZone()
            },
            onEventStatusChange = { event, newStatus, timeSpent ->
                val updatedEvent = event.copy(
                    status = newStatus,
                    timeSpentHours = timeSpent,
                    updatedAt = LocalDateTime.now()
                )
                viewModel.updateEvent(updatedEvent)
            }
        )
        eventsRecycler.layoutManager = LinearLayoutManager(requireContext())
        eventsRecycler.adapter = eventAdapter
        
        // 设置事件拖拽功能
        val itemTouchHelper = ItemTouchHelper(
            EventItemTouchHelper(
                onItemMove = { fromPosition, toPosition ->
                    // 更新事件位置
                    val selectedTask = viewModel.selectedTask.value
                    selectedTask?.let { taskWithEvents ->
                        val events = taskWithEvents.events.toMutableList()
                        if (fromPosition < events.size && toPosition < events.size) {
                            val fromEvent = events[fromPosition]
                            val toEvent = events[toPosition]
                            // 通知ViewModel更新事件位置
                            viewModel.moveEvent(fromEvent.id, toEvent.position)
                        }
                    }
                    eventAdapter.moveItem(fromPosition, toPosition)
                },
                onItemDismiss = { position ->
                    // 滑动删除事件
                    eventAdapter.getItemAt(position)?.let { event ->
                        showDeleteEventDialog(event)
                    }
                },
                onDeleteZoneEntered = {
                    // 显示删除区域并高亮
                    showDeleteZone()
                    highlightDeleteZone()
                },
                onDeleteZoneExited = {
                    // 重置删除区域样式
                    resetDeleteZone()
                },
                onItemDeleted = { position ->
                    // 拖拽到删除区域删除事件
                    eventAdapter.getItemAt(position)?.let { event ->
                        showDeleteEventDialog(event)
                    }
                }
            )
        )
        itemTouchHelper.attachToRecyclerView(eventsRecycler)
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            showCreateTaskDialog()
        }

        fabAddEvent.setOnClickListener {
            val selectedTask = viewModel.selectedTask.value
            selectedTask?.let {
                showCreateEventDialog(it.task.id)
            }
        }
        
        sidebarToggle.setOnClickListener {
            toggleSidebar()
        }
        
        sidebarOverlay.setOnClickListener {
            hideSidebar()
        }
        
        // Setup statistics click listeners
        totalStatsContainer.setOnClickListener {
            setEventFilter(null)
        }
        
        completedStatsContainer.setOnClickListener {
            setEventFilter(EventStatus.COMPLETED)
        }
        
        inProgressStatsContainer.setOnClickListener {
            setEventFilter(EventStatus.IN_PROGRESS)
        }
        
        pendingStatsContainer.setOnClickListener {
            setEventFilter(EventStatus.PENDING)
        }
    }
    
    private fun toggleSidebar() {
        if (isSidebarVisible) {
            hideSidebar()
        } else {
            showSidebar()
        }
    }
    
    private fun showSidebar() {
        if (isSidebarVisible || currentAnimator?.isRunning == true) return
        
        isSidebarVisible = true
        sidebarOverlay.visibility = View.VISIBLE
        
        currentAnimator = ValueAnimator.ofFloat(-sidebarWidth.toFloat(), 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                val translationX = animation.animatedValue as Float
                sidebarContainer.translationX = translationX
                
                // 遮罩透明度动画
                val progress = (translationX + sidebarWidth) / sidebarWidth
                sidebarOverlay.alpha = progress * 0.5f
            }
            
            start()
        }
    }
    
    private fun hideSidebar() {
        if (!isSidebarVisible || currentAnimator?.isRunning == true) return
        
        isSidebarVisible = false
        
        currentAnimator = ValueAnimator.ofFloat(sidebarContainer.translationX, -sidebarWidth.toFloat()).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                val translationX = animation.animatedValue as Float
                sidebarContainer.translationX = translationX
                
                // 遮罩透明度动画
                val progress = (translationX + sidebarWidth) / sidebarWidth
                sidebarOverlay.alpha = progress * 0.5f
                
                if (progress <= 0f) {
                    sidebarOverlay.visibility = View.GONE
                }
            }
            
            start()
        }
    }

    private fun showDeleteZone() {
        if (isDeleteZoneVisible) return
        
        isDeleteZoneVisible = true
        deleteZone.visibility = View.VISIBLE
        
        // 添加动画效果
        deleteZone.scaleY = 0.5f
        deleteZone.alpha = 0f
        deleteZone.animate()
            .alpha(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
    
    private fun hideDeleteZone() {
        if (!isDeleteZoneVisible) return
        
        isDeleteZoneVisible = false
        
        // 添加动画效果
        deleteZone.animate()
            .alpha(0f)
            .scaleY(0.5f)
            .setDuration(200)
            .withEndAction {
                deleteZone.visibility = View.GONE
            }
            .start()
    }
    
    private fun highlightDeleteZone() {
        // 高亮删除区域表示可以删除
        deleteZone.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(100)
            .withEndAction {
                deleteZone.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun resetDeleteZone() {
        // 重置删除区域样式
        deleteZone.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(100)
            .start()
    }

    private fun observeData() {
        // 观察任务列表
        viewModel.tasksWithEvents.observe(viewLifecycleOwner) { tasksWithEvents ->
            taskAdapter.submitList(tasksWithEvents)
        }

        // 观察选中的任务
        viewModel.selectedTask.observe(viewLifecycleOwner) { selectedTaskWithEvents ->
            if (selectedTaskWithEvents != null) {
                selectedTaskTitle.text = selectedTaskWithEvents.task.title
                applyEventFilter(selectedTaskWithEvents.events)
                eventsRecycler.visibility = View.VISIBLE
                emptyStateContainer.visibility = View.GONE
                fabAddEvent.visibility = View.VISIBLE
                
                // 显示和更新事件统计
                eventsStatsContainer.visibility = View.VISIBLE
                updateEventStats(selectedTaskWithEvents)
                
                // 更新任务适配器的选中状态
                taskAdapter.setSelectedTask(selectedTaskWithEvents.task.id)
            } else {
                selectedTaskTitle.text = "请选择一个任务"
                eventAdapter.submitList(emptyList())
                eventsRecycler.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
                fabAddEvent.visibility = View.GONE
                eventsStatsContainer.visibility = View.GONE
                
                taskAdapter.setSelectedTask(null)
            }
        }
    }
    
    private fun updateEventStats(taskWithEvents: TaskWithEvents) {
        val events = taskWithEvents.events
        val totalCount = events.size
        val completedCount = events.count { it.status == EventStatus.COMPLETED }
        val inProgressCount = events.count { it.status == EventStatus.IN_PROGRESS }
        val pendingCount = events.count { it.status == EventStatus.PENDING }
        
        totalEventsText.text = "总计: $totalCount"
        completedEventsText.text = "完成: $completedCount"
        inProgressEventsText.text = "进行中: $inProgressCount"
        pendingEventsText.text = "待处理: $pendingCount"
        
        // Update visual feedback for selected filter
        updateFilterVisualFeedback()
    }
    
    private fun setEventFilter(filter: EventStatus?) {
        currentFilter = filter
        
        // Apply filter to current events
        val selectedTask = viewModel.selectedTask.value
        selectedTask?.let { taskWithEvents ->
            applyEventFilter(taskWithEvents.events)
        }
        
        // Update visual feedback
        updateFilterVisualFeedback()
    }
    
    private fun applyEventFilter(events: List<Event>) {
        val filteredEvents = if (currentFilter == null) {
            events
        } else {
            events.filter { it.status == currentFilter }
        }
        eventAdapter.submitList(filteredEvents)
    }
    
    private fun updateFilterVisualFeedback() {
        // Reset all containers to normal state
        totalStatsContainer.alpha = if (currentFilter == null) 1.0f else 0.6f
        completedStatsContainer.alpha = if (currentFilter == EventStatus.COMPLETED) 1.0f else 0.6f
        inProgressStatsContainer.alpha = if (currentFilter == EventStatus.IN_PROGRESS) 1.0f else 0.6f
        pendingStatsContainer.alpha = if (currentFilter == EventStatus.PENDING) 1.0f else 0.6f
    }

    private fun showCreateTaskDialog(existingTask: Task? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_task, null)
        
        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.task_title)
        val descEdit = dialogView.findViewById<TextInputEditText>(R.id.task_description)
        
        // 如果是编辑任务，填入现有数据
        existingTask?.let { task ->
            titleEdit.setText(task.title)
            descEdit.setText(task.description)
        }
        
        val isEditing = existingTask != null
        val dialogTitle = if (isEditing) "编辑任务" else "创建新任务"
        val buttonText = if (isEditing) "保存" else "创建"
        
        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(buttonText) { _, _ ->
                val title = titleEdit.text.toString().trim()
                val desc = descEdit.text.toString().trim()
                if (title.isNotBlank()) {
                    if (isEditing) {
                        val updatedTask = existingTask!!.copy(
                            title = title,
                            description = desc
                        )
                        viewModel.updateTask(updatedTask)
                    } else {
                        viewModel.createTask(title, desc)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCreateEventDialog(taskId: Long, existingEvent: Event? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_event, null)
        
        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.event_title)
        val descEdit = dialogView.findViewById<TextInputEditText>(R.id.event_description)
        val priorityGroup = dialogView.findViewById<RadioGroup>(R.id.priority_group)
        val estimatedDurationEdit = dialogView.findViewById<TextInputEditText>(R.id.estimated_duration)
        val hasDueDateCheck = dialogView.findViewById<CheckBox>(R.id.has_due_date)
        val dueDateButton = dialogView.findViewById<Button>(R.id.due_date_button)
        
        var selectedDueDate: LocalDateTime? = existingEvent?.dueDate
        
        // 如果是编辑现有事件，预填充数据
        existingEvent?.let { event ->
            titleEdit.setText(event.title)
            descEdit.setText(event.description)
            estimatedDurationEdit.setText(event.estimatedDuration.toString())
            
            // 设置优先级
            val priorityRadioId = when (event.priority) {
                Priority.LOW -> R.id.priority_low
                Priority.MEDIUM -> R.id.priority_medium
                Priority.HIGH -> R.id.priority_high
                Priority.URGENT -> R.id.priority_urgent
            }
            priorityGroup.check(priorityRadioId)
            
            // 设置截止日期
            if (event.dueDate != null) {
                hasDueDateCheck.isChecked = true
                dueDateButton.isEnabled = true
                dueDateButton.text = event.dueDate.toLocalDate().toString()
            }
        }
        
        hasDueDateCheck.setOnCheckedChangeListener { _, isChecked ->
            dueDateButton.isEnabled = isChecked
            if (!isChecked) {
                selectedDueDate = null
                dueDateButton.text = "选择日期"
            }
        }
        
        dueDateButton.setOnClickListener {
            showDatePicker { date ->
                selectedDueDate = date.atTime(23, 59)
                dueDateButton.text = date.toString()
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(if (existingEvent != null) "编辑事件" else "创建新事件")
            .setView(dialogView)
            .setPositiveButton(if (existingEvent != null) "更新" else "创建") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val desc = descEdit.text.toString().trim()
                
                if (title.isNotBlank()) {
                    val priority = when (priorityGroup.checkedRadioButtonId) {
                        R.id.priority_low -> Priority.LOW
                        R.id.priority_medium -> Priority.MEDIUM
                        R.id.priority_high -> Priority.HIGH
                        R.id.priority_urgent -> Priority.URGENT
                        else -> Priority.MEDIUM
                    }
                    
                    val estimatedDuration = estimatedDurationEdit.text.toString().toIntOrNull() ?: 0
                    
                    if (existingEvent != null) {
                        // 更新现有事件
                        val updatedEvent = existingEvent.copy(
                            title = title,
                            description = desc,
                            priority = priority,
                            estimatedDuration = estimatedDuration,
                            dueDate = selectedDueDate,
                            updatedAt = LocalDateTime.now()
                        )
                        viewModel.updateEvent(updatedEvent)
                    } else {
                        // 创建新事件
                        viewModel.createEvent(taskId, title, desc, priority, estimatedDuration, selectedDueDate)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTaskStatusDialog(task: Task) {
        // 首先选择任务显示其事件
        viewModel.selectTask(task.id)
        
        // 根据当前状态编制可用选项
        val availableStatuses = when (task.status) {
            TaskStatus.PENDING -> listOf(TaskStatus.IN_PROGRESS)
            TaskStatus.IN_PROGRESS -> listOf(TaskStatus.COMPLETED, TaskStatus.PENDING)
            TaskStatus.COMPLETED -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.PENDING)
        }
        
        val statusOptions = availableStatuses.map { it.displayName }.toTypedArray()
        
        if (statusOptions.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("变更任务状态: ${task.title}")
                .setMessage("当前状态: ${task.status.displayName}\n用时: ${String.format("%.1f", task.timeSpentHours)}小时")
                .setItems(statusOptions) { _, which ->
                    val newStatus = availableStatuses[which]
                    val updatedTask = task.copy(status = newStatus)
                    viewModel.updateTask(updatedTask)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            // 没有可用的状态变更，显示信息
            AlertDialog.Builder(requireContext())
                .setTitle(task.title)
                .setMessage("当前状态: ${task.status.displayName}\n用时: ${String.format("%.1f", task.timeSpentHours)}小时")
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun showEventStatusDialog(event: Event) {
        // 根据当前状态编制可用选项
        val availableStatuses = when (event.status) {
            EventStatus.PENDING -> listOf(EventStatus.IN_PROGRESS)
            EventStatus.IN_PROGRESS -> listOf(EventStatus.COMPLETED, EventStatus.PENDING)
            EventStatus.COMPLETED -> listOf(EventStatus.IN_PROGRESS, EventStatus.PENDING)
            EventStatus.CANCELLED -> listOf(EventStatus.PENDING) // 可以从取消恢复到待处理
        }
        
        val statusOptions = availableStatuses.map { it.displayName }.toTypedArray()
        
        if (statusOptions.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("变更事件状态: ${event.title}")
                .setMessage("当前状态: ${event.status.displayName}\n用时: ${String.format("%.1f", event.timeSpentHours)}小时")
                .setItems(statusOptions) { _, which ->
                    val newStatus = availableStatuses[which]
                    
                    // 如果变更为进行中或完成，显示时间输入对话框
                    if ((event.status == EventStatus.PENDING && newStatus == EventStatus.IN_PROGRESS) ||
                        (event.status == EventStatus.IN_PROGRESS && newStatus == EventStatus.COMPLETED)) {
                        showTimeInputDialog(event, newStatus)
                    } else {
                        viewModel.updateEventStatus(event.id, newStatus)
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            // 没有可用的状态变更，显示信息
            AlertDialog.Builder(requireContext())
                .setTitle(event.title)
                .setMessage("当前状态: ${event.status.displayName}\n用时: ${String.format("%.1f", event.timeSpentHours)}小时")
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun showTimeInputDialog(event: Event, newStatus: EventStatus) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_time_input, null)
        
        val timeInput = dialogView.findViewById<EditText>(R.id.time_input)
        val messageText = when (newStatus) {
            EventStatus.IN_PROGRESS -> "请输入已用时间（小时）："
            EventStatus.COMPLETED -> "请输入完成时的总用时（小时）："
            else -> "请输入用时（小时）："
        }
        
        // 预填充当前用时
        timeInput.setText(event.timeSpentHours.toString())
        
        AlertDialog.Builder(requireContext())
            .setTitle("更新事件: ${event.title}")
            .setMessage(messageText)
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val timeSpent = timeInput.text.toString().toDoubleOrNull() ?: event.timeSpentHours
                val updatedEvent = event.copy(
                    status = newStatus,
                    timeSpentHours = timeSpent
                )
                viewModel.updateEvent(updatedEvent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEventContextMenu(event: Event) {
        val options = arrayOf("编辑", "删除")
        
        AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditEventDialog(event)
                    1 -> showDeleteEventDialog(event)
                }
            }
            .show()
    }

    private fun showEditEventDialog(event: Event) {
        showCreateEventDialog(event.taskId, event)
    }

    private fun showDeleteEventDialog(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除事件")
            .setMessage("确定要删除事件「${event.title}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteEvent(event.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTaskContextMenu(taskWithEvents: TaskWithEvents) {
        val options = arrayOf("编辑", "删除")
        
        AlertDialog.Builder(requireContext())
            .setTitle(taskWithEvents.task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTaskDialog(taskWithEvents.task)
                    1 -> showDeleteTaskDialog(taskWithEvents.task)
                }
            }
            .show()
    }

    private fun showEditTaskDialog(task: Task) {
        showCreateTaskDialog(task)
    }

    private fun showDeleteTaskDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除任务")
            .setMessage("确定要删除任务「${task.title}」吗？删除任务将同时删除其所有相关事件。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (LocalDate) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun showEventDetailsDialog(event: Event) {
        // 这个方法保留以兼容其他可能的调用，但默认调用状态对话框
        showEventStatusDialog(event)
    }
}