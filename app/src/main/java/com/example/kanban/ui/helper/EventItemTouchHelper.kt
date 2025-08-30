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
                // 拖拽开始时显示删除区域
                onDeleteZoneEntered()
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                // 拖拽结束时确保隐藏删除区域
                onDeleteZoneExited()
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        
        // 恢复原始状态，但保持MaterialCardView的elevation
        viewHolder.itemView.apply {
            scaleX = 1.0f
            scaleY = 1.0f
            alpha = 1.0f
            translationX = 0f
            translationY = 0f
        }
        
        // 如果在删除区域释放，则删除项目
        if (shouldDelete && draggedPosition != -1) {
            onItemDeleted(draggedPosition)
        }
        
        // 拖拽结束时总是隐藏删除区域
        onDeleteZoneExited()
        
        // 重置所有状态
        isOverDeleteZone = false
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
        // 获取父容器的坐标
        val parent = recyclerView.parent as? View ?: return
        
        // 计算删除区域在父容器中的位置（底部80dp高度区域）
        val deleteZoneTop = parent.height - 80 * parent.context.resources.displayMetrics.density
        
        // 获取itemView在父容器中的全局坐标
        val location = IntArray(2)
        itemView.getLocationInWindow(location)
        val parentLocation = IntArray(2)
        parent.getLocationInWindow(parentLocation)
        
        // 计算itemView相对于父容器的bottom位置
        val itemBottomInParent = location[1] - parentLocation[1] + itemView.height
        
        // 检查是否进入删除区域
        val isNowOverDeleteZone = itemBottomInParent > deleteZoneTop
        
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