package com.example.kanban.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kanban.R
import com.example.kanban.data.entity.TaskWithEvents
import com.example.kanban.data.entity.EventStatus
import com.example.kanban.data.entity.TaskStatus

class TaskAdapter(
    private val onTaskClick: (TaskWithEvents) -> Unit,
    private val onTaskLongClick: (TaskWithEvents) -> Unit = {}
) : ListAdapter<TaskWithEvents, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var selectedTaskId: Long? = null

    fun setSelectedTask(taskId: Long?) {
        val oldSelectedTaskId = selectedTaskId
        selectedTaskId = taskId
        
        // 刷新之前选中的项目
        if (oldSelectedTaskId != null) {
            val oldIndex = currentList.indexOfFirst { it.task.id == oldSelectedTaskId }
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex)
            }
        }
        
        // 刷新新选中的项目
        if (taskId != null) {
            val newIndex = currentList.indexOfFirst { it.task.id == taskId }
            if (newIndex != -1) {
                notifyItemChanged(newIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_sidebar, parent, false)
        return TaskViewHolder(view, onTaskClick, onTaskLongClick)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskWithEvents = getItem(position)
        holder.bind(taskWithEvents, selectedTaskId == taskWithEvents.task.id)
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskClick: (TaskWithEvents) -> Unit,
        private val onTaskLongClick: (TaskWithEvents) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val titleText: TextView = itemView.findViewById(R.id.task_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.task_description)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.task_progress)
        private val eventsCountText: TextView = itemView.findViewById(R.id.events_count)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)

        fun bind(taskWithEvents: TaskWithEvents, isSelected: Boolean) {
            val task = taskWithEvents.task
            val events = taskWithEvents.events

            titleText.text = task.title
            
            // 显示或隐藏描述
            if (task.description.isNotBlank()) {
                descriptionText.text = task.description
                descriptionText.visibility = View.VISIBLE
            } else {
                descriptionText.visibility = View.GONE
            }

            // 设置进度条
            val progress = (taskWithEvents.progress * 100).toInt()
            progressBar.progress = progress

            // 设置事件计数和时间信息
            val completedCount = taskWithEvents.completedEventsCount
            val totalCount = taskWithEvents.totalEventsCount
            val timeSpentText = if (task.timeSpentHours > 0) {
                " (${String.format("%.1f", task.timeSpentHours)}h)"
            } else {
                ""
            }
            eventsCountText.text = "$completedCount/$totalCount$timeSpentText"
            
            // 根据完成情况设置文本颜色
            val textColor = when {
                totalCount == 0 -> itemView.context.getColor(R.color.text_tertiary)
                completedCount == totalCount -> itemView.context.getColor(R.color.status_completed)
                else -> itemView.context.getColor(R.color.status_in_progress)
            }
            eventsCountText.setTextColor(textColor)

            // 设置状态指示器和文本（使用任务的状态）
            when (task.status) {
                TaskStatus.PENDING -> {
                    statusIndicator.setBackgroundColor(itemView.context.getColor(R.color.status_pending))
                    statusText.text = "待处理"
                    statusText.setTextColor(itemView.context.getColor(R.color.status_pending))
                }
                TaskStatus.IN_PROGRESS -> {
                    statusIndicator.setBackgroundColor(itemView.context.getColor(R.color.status_in_progress))
                    statusText.text = "进行中"
                    statusText.setTextColor(itemView.context.getColor(R.color.status_in_progress))
                }
                TaskStatus.COMPLETED -> {
                    statusIndicator.setBackgroundColor(itemView.context.getColor(R.color.status_completed))
                    statusText.text = "已完成"
                    statusText.setTextColor(itemView.context.getColor(R.color.status_completed))
                }
            }

            // 设置选中状态的背景
            itemView.alpha = if (isSelected) 0.8f else 1.0f
            itemView.scaleX = if (isSelected) 0.95f else 1.0f
            itemView.scaleY = if (isSelected) 0.95f else 1.0f

            // 点击事件
            itemView.setOnClickListener {
                // 添加点击反馈动画
                itemView.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start()
                    }
                    .start()
                onTaskClick(taskWithEvents)
            }
            
            // 长按事件
            itemView.setOnLongClickListener {
                onTaskLongClick(taskWithEvents)
                true
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskWithEvents>() {
        override fun areItemsTheSame(oldItem: TaskWithEvents, newItem: TaskWithEvents): Boolean {
            return oldItem.task.id == newItem.task.id
        }

        override fun areContentsTheSame(oldItem: TaskWithEvents, newItem: TaskWithEvents): Boolean {
            return oldItem.task == newItem.task && 
                   oldItem.events.size == newItem.events.size &&
                   oldItem.events.zip(newItem.events).all { (old, new) -> old == new }
        }
    }
}