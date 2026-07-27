/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.drag

import android.graphics.Canvas
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.puder.trs80.ConfigurationListViewAdapter
import kotlin.math.abs

/**
 * Drag-and-drop behaviour for the configuration list. Items can be reordered by long-pressing
 * them, but not dismissed by swiping.
 *
 * @param adapter the adapter that is told about the moves this callback detects.
 */
class ConfigurationItemTouchHelperCallback(
        private val adapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder): Int {
        if (!(viewHolder as ConfigurationListViewAdapter.Holder).draggable) {
            return 0
        }
        // A grid can be dragged sideways as well; a list only up and down.
        val dragFlags = if (recyclerView.layoutManager is GridLayoutManager) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        return makeMovementFlags(dragFlags, NO_SWIPE_FLAGS)
    }

    override fun chooseDropTarget(
            selected: RecyclerView.ViewHolder,
            dropTargets: List<RecyclerView.ViewHolder>,
            curX: Int,
            curY: Int): RecyclerView.ViewHolder? =
            // If the default heuristic cannot decide but there is only one candidate, take it.
            super.chooseDropTarget(selected, dropTargets, curX, curY) ?: dropTargets.singleOrNull()

    override fun onMove(
            recyclerView: RecyclerView,
            source: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder): Boolean =
            source.itemViewType == target.itemViewType &&
                    adapter.onItemMove(source.adapterPosition, target.adapterPosition)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        // Fade the view out as it is swiped out of the parent's bounds.
        viewHolder.itemView.alpha = ALPHA_FULL - abs(dX) / viewHolder.itemView.width
        viewHolder.itemView.translationX = dX
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = DRAGGED_ALPHA
        }
        // Only the active item should change.
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            (viewHolder as? ItemTouchHelperViewHolder)?.onItemSelected()
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        viewHolder.itemView.alpha = ALPHA_FULL
        (viewHolder as? ItemTouchHelperViewHolder)?.onItemClear()
    }

    private companion object {
        private const val ALPHA_FULL = 1.0f
        private const val DRAGGED_ALPHA = 0.5f
        private const val NO_SWIPE_FLAGS = 0
    }
}
