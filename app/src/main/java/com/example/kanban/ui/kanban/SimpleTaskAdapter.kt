package com.example.kanban.ui.kanban

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kanban.R
import com.example.kanban.data.entity.Task

class SimpleTaskAdapter(
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<SimpleTaskAdapter.TaskViewHolder>() {

    private var tasks = listOf<Task>()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_simple, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.task_title)
        private val descView: TextView = itemView.findViewById(R.id.task_description)

        fun bind(task: Task) {
            titleView.text = task.title
            descView.text = task.description
            
            itemView.setOnClickListener {
                onTaskClick(task)
            }
        }
    }
}