package com.example.kanban.ui.helper

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanban.ui.adapter.EventAdapter

class EventItemTouchHelper(
    private val onItemMove: (fromPosition: Int, toPosition: Int) -> Unit,
    private val onItemDismiss: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // 支持上下拖拽
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT  // 支持左右滑动删除
) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        onItemMove(fromPosition, toPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onItemDismiss(position)
    }

    override fun isLongPressDragEnabled(): Boolean = true
    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                // 拖拽时增加阴影和缩放效果
                viewHolder?.itemView?.apply {
                    elevation = 8f
                    scaleX = 1.05f
                    scaleY = 1.05f
                    alpha = 0.8f
                }
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        
        // 恢复原始状态
        viewHolder.itemView.apply {
            elevation = 0f
            scaleX = 1.0f
            scaleY = 1.0f
            alpha = 1.0f
        }
    }
}