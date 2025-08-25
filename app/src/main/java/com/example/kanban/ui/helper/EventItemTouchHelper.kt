package com.example.kanban.ui.helper

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanban.ui.adapter.EventAdapter

class EventItemTouchHelper(
    private val onItemMove: (fromPosition: Int, toPosition: Int) -> Unit,
    private val onItemDismiss: (position: Int) -> Unit,
    private val onDeleteZoneEntered: () -> Unit = {},
    private val onDeleteZoneExited: () -> Unit = {},
    private val onItemDeleted: (position: Int) -> Unit = {}
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // 支持上下拖拽
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT  // 支持左右滑动删除
) {

    private var isOverDeleteZone = false
    private var draggedPosition = -1
    private var shouldDelete = false

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
                    elevation = 16f  // 提高elevation确保在删除区域上方
                    scaleX = 1.05f
                    scaleY = 1.05f
                    alpha = 0.9f
                }
                // 记录被拖拽的item位置
                draggedPosition = viewHolder?.adapterPosition ?: -1
                shouldDelete = false
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
        
        // 如果在删除区域释放，则删除项目
        if (shouldDelete && draggedPosition != -1) {
            onItemDeleted(draggedPosition)
        }
        
        // 重置删除区域状态
        if (isOverDeleteZone) {
            isOverDeleteZone = false
            onDeleteZoneExited()
        }
        
        // 重置拖拽位置和删除标志
        draggedPosition = -1
        shouldDelete = false
    }
    
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            // 检查是否拖拽到删除区域
            checkDeleteZone(viewHolder.itemView, recyclerView)
        }
    }
    
    private fun checkDeleteZone(itemView: View, recyclerView: RecyclerView) {
        // 获取删除区域的位置（假设在recyclerView的底部）
        val deleteZoneTop = recyclerView.height - 80 // 80dp is delete zone height
        
        // 获取itemView的底部位置
        val itemBottom = itemView.y + itemView.height
        
        // 检查是否进入删除区域
        val isNowOverDeleteZone = itemBottom > deleteZoneTop
        
        if (isNowOverDeleteZone && !isOverDeleteZone) {
            // 进入删除区域
            isOverDeleteZone = true
            shouldDelete = true
            onDeleteZoneEntered()
        } else if (!isNowOverDeleteZone && isOverDeleteZone) {
            // 离开删除区域
            isOverDeleteZone = false
            shouldDelete = false
            onDeleteZoneExited()
        }
    }
}