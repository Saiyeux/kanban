package com.example.kanban.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.widget.Button
import com.example.kanban.R
import com.example.kanban.data.entity.Event
import com.example.kanban.data.entity.Priority
import com.example.kanban.data.entity.EventStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onEventEdit: (Event) -> Unit = {},
    private val onEventDelete: (Event) -> Unit = {},
    private val onEventLongClick: (Event) -> Unit = {},
    private val onEventStatusChange: (Event, EventStatus, Double) -> Unit = { _, _, _ -> }
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {
    
    private var expandedEventId: Long = -1

    // 拖拽相关方法
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val list = currentList.toMutableList()
        val item = list.removeAt(fromPosition)
        list.add(toPosition, item)
        submitList(list)
    }

    fun getItemAt(position: Int): Event? {
        return if (position >= 0 && position < itemCount) {
            getItem(position)
        } else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view, onEventClick, onEventEdit, onEventDelete, onEventLongClick, onEventStatusChange)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        val isExpanded = expandedEventId == event.id
        holder.bind(event, isExpanded) { eventId ->
            expandedEventId = if (expandedEventId == eventId) -1 else eventId
            notifyDataSetChanged()
        }
    }

    class EventViewHolder(
        itemView: View,
        private val onEventClick: (Event) -> Unit,
        private val onEventEdit: (Event) -> Unit,
        private val onEventDelete: (Event) -> Unit,
        private val onEventLongClick: (Event) -> Unit,
        private val onEventStatusChange: (Event, EventStatus, Double) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val priorityIndicator: View = itemView.findViewById(R.id.priority_indicator)
        private val titleText: TextView = itemView.findViewById(R.id.event_title)
        private val statusText: TextView = itemView.findViewById(R.id.event_status)
        private val descriptionText: TextView = itemView.findViewById(R.id.event_description)
        private val estimatedTimeContainer: View = itemView.findViewById(R.id.estimated_time_container)
        private val estimatedDurationText: TextView = itemView.findViewById(R.id.estimated_duration)
        private val actualTimeContainer: View = itemView.findViewById(R.id.actual_time_container)
        private val actualDurationText: TextView = itemView.findViewById(R.id.actual_duration)
        private val dueDateText: TextView = itemView.findViewById(R.id.due_date)
        private val priorityText: TextView = itemView.findViewById(R.id.priority_text)
        private val dragHandle: View = itemView.findViewById(R.id.drag_handle)
        private val editButton: View = itemView.findViewById(R.id.edit_button)
        private val statusButtonsContainer: View = itemView.findViewById(R.id.status_buttons_container)
        private val btnStatus1: View = itemView.findViewById(R.id.btn_status_1)
        private val btnStatus2: View = itemView.findViewById(R.id.btn_status_2)

        fun bind(event: Event, isExpanded: Boolean, onToggleExpand: (Long) -> Unit) {
            titleText.text = event.title
            
            // 显示或隐藏描述
            if (event.description.isNotBlank()) {
                descriptionText.text = event.description
                descriptionText.visibility = View.VISIBLE
            } else {
                descriptionText.visibility = View.GONE
            }

            // 设置优先级指示器和文本
            setPriority(event.priority)

            // 设置状态
            setStatus(event.status)

            // 设置时间信息
            setTimeInfo(event)

            // 设置截止日期
            setDueDate(event.dueDate)

            // 点击事件 - 切换展开/收起状态按钮
            itemView.setOnClickListener {
                // 添加点击反馈动画
                itemView.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
                onToggleExpand(event.id)
            }
            
            // 长按事件 - 显示删除区域
            itemView.setOnLongClickListener {
                onEventLongClick(event)
                true
            }
            
            // 设置状态按钮的可见性和点击事件
            setupStatusButtons(event, isExpanded)

            // 编辑按钮点击事件
            editButton.setOnClickListener {
                onEventEdit(event)
            }
        }
        
        private fun setupStatusButtons(event: Event, isExpanded: Boolean) {
            if (isExpanded) {
                statusButtonsContainer.visibility = View.VISIBLE
                // 添加展开动画
                statusButtonsContainer.alpha = 0f
                statusButtonsContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                
                // 根据当前状态设置可用按钮
                val availableStatuses = when (event.status) {
                    EventStatus.PENDING -> listOf(EventStatus.IN_PROGRESS)
                    EventStatus.IN_PROGRESS -> listOf(EventStatus.COMPLETED, EventStatus.PENDING)
                    EventStatus.COMPLETED -> listOf(EventStatus.IN_PROGRESS, EventStatus.PENDING)
                    EventStatus.CANCELLED -> listOf(EventStatus.PENDING)
                }
                
                // 设置按钮1
                if (availableStatuses.isNotEmpty()) {
                    btnStatus1.visibility = View.VISIBLE
                    (btnStatus1 as Button).text = availableStatuses[0].displayName
                    btnStatus1.setOnClickListener {
                        handleStatusChange(event, availableStatuses[0])
                    }
                } else {
                    btnStatus1.visibility = View.GONE
                }
                
                // 设置按钮2
                if (availableStatuses.size > 1) {
                    btnStatus2.visibility = View.VISIBLE
                    (btnStatus2 as Button).text = availableStatuses[1].displayName
                    btnStatus2.setOnClickListener {
                        handleStatusChange(event, availableStatuses[1])
                    }
                } else {
                    btnStatus2.visibility = View.GONE
                }
            } else {
                if (statusButtonsContainer.visibility == View.VISIBLE) {
                    // 添加收起动画
                    statusButtonsContainer.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            statusButtonsContainer.visibility = View.GONE
                        }
                        .start()
                } else {
                    statusButtonsContainer.visibility = View.GONE
                }
            }
        }
        
        private fun handleStatusChange(event: Event, newStatus: EventStatus) {
            // 只有从进行中变更为完成时才显示时间输入对话框
            if (event.status == EventStatus.IN_PROGRESS && newStatus == EventStatus.COMPLETED) {
                showTimeInputDialog(event, newStatus)
            } else {
                onEventStatusChange(event, newStatus, event.timeSpentHours)
            }
        }
        
        private fun showTimeInputDialog(event: Event, newStatus: EventStatus) {
            val context = itemView.context
            val input = EditText(context)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.setText(event.timeSpentHours.toString())
            
            val message = "请输入完成时的总用时（小时）："
            
            AlertDialog.Builder(context)
                .setTitle("更新事件: ${event.title}")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("确定") { _, _ ->
                    val timeSpent = input.text.toString().toDoubleOrNull() ?: event.timeSpentHours
                    onEventStatusChange(event, newStatus, timeSpent)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        private fun setPriority(priority: Priority) {
            val color = when (priority) {
                Priority.LOW -> Color.parseColor("#4CAF50")
                Priority.MEDIUM -> Color.parseColor("#FF9800")
                Priority.HIGH -> Color.parseColor("#F44336")
                Priority.URGENT -> Color.parseColor("#9C27B0")
            }
            
            priorityIndicator.setBackgroundColor(color)
            priorityText.text = priority.displayName
            priorityText.setTextColor(color)
        }

        private fun setStatus(status: EventStatus) {
            val (textColor, background) = when (status) {
                EventStatus.PENDING -> Pair(
                    itemView.context.getColor(R.color.status_pending),
                    R.drawable.bg_status_pending
                )
                EventStatus.IN_PROGRESS -> Pair(
                    itemView.context.getColor(R.color.status_in_progress),
                    R.drawable.bg_status_in_progress
                )
                EventStatus.COMPLETED -> Pair(
                    itemView.context.getColor(R.color.status_completed),
                    R.drawable.bg_status_completed
                )
                EventStatus.CANCELLED -> Pair(
                    itemView.context.getColor(R.color.status_cancelled),
                    R.drawable.bg_status_pending // 使用同一样式
                )
            }
            
            statusText.text = status.displayName
            statusText.setTextColor(textColor)
            statusText.setBackgroundResource(background)
        }

        private fun setTimeInfo(event: Event) {
            // 预计时间
            if (event.estimatedDuration > 0) {
                estimatedDurationText.text = formatDuration(event.estimatedDuration)
                estimatedTimeContainer.visibility = View.VISIBLE
            } else {
                estimatedTimeContainer.visibility = View.GONE
            }

            // 实际用时（优先显示小时，其次是分钟）
            if (event.timeSpentHours > 0) {
                actualDurationText.text = String.format("%.1fh", event.timeSpentHours)
                actualTimeContainer.visibility = View.VISIBLE
            } else if (event.actualDuration > 0) {
                actualDurationText.text = formatDuration(event.actualDuration)
                actualTimeContainer.visibility = View.VISIBLE
            } else {
                actualTimeContainer.visibility = View.GONE
            }
        }

        private fun setDueDate(dueDate: LocalDateTime?) {
            if (dueDate != null) {
                val formatter = DateTimeFormatter.ofPattern("MM-dd")
                dueDateText.text = dueDate.format(formatter)
                dueDateText.visibility = View.VISIBLE

                // 检查是否过期
                if (dueDate.isBefore(LocalDateTime.now())) {
                    dueDateText.setTextColor(Color.parseColor("#F44336"))
                } else {
                    dueDateText.setTextColor(Color.parseColor("#757575"))
                }
            } else {
                dueDateText.visibility = View.GONE
            }
        }

        private fun formatDuration(minutes: Int): String {
            return when {
                minutes < 60 -> "${minutes}min"
                minutes < 1440 -> "${minutes / 60}h ${minutes % 60}min"
                else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}