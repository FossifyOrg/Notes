package org.fossify.notes.adapters

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.TypedValue
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.removeBit
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.notes.R
import org.fossify.notes.databinding.ItemChecklistBinding
import org.fossify.notes.dialogs.EditTaskDialog
import org.fossify.notes.extensions.config
import org.fossify.notes.extensions.getPercentageFontSize
import org.fossify.notes.helpers.DONE_CHECKLIST_ITEM_ALPHA
import org.fossify.notes.interfaces.TasksActionListener
import org.fossify.notes.models.Task
import java.util.Collections

class TasksAdapter(
    activity: BaseSimpleActivity,
    val listener: TasksActionListener?,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit = {},
) : MyRecyclerViewListAdapter<Task>(
    activity = activity, recyclerView = recyclerView, diffUtil = TaskDiffCallback(), itemClick = itemClick
), ItemTouchHelperContract {

    private var touchHelper: ItemTouchHelper? = null
    private var startReorderDragListener: StartReorderDragListener

    init {
        setupDragListener(true)
        setHasStableIds(true)

        touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper!!.attachToRecyclerView(recyclerView)

        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper?.startDrag(viewHolder)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_checklist

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_move_to_top -> moveSelectedItemsToTop()
            R.id.cab_move_to_bottom -> moveSelectedItemsToBottom()
            R.id.cab_rename -> renameChecklistItem()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getItemId(position: Int) = currentList[position].id.toLong()

    override fun getSelectableItemCount() = currentList.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = currentList.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.id == key }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeCreated() = notifyDataSetChanged()

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() = notifyDataSetChanged()

    override fun prepareActionMode(menu: Menu) {
        val selectedItems = getSelectedItems()
        if (selectedItems.isEmpty()) {
            return
        }

        menu.findItem(R.id.cab_rename).isVisible = isOneItemSelected()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemChecklistBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindView(item, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, item, holder)
        }

        bindViewHolder(holder)
    }

    private fun renameChecklistItem() {
        val task = getSelectedItems().first()
        EditTaskDialog(activity, task.title) { title ->
            val tasks = currentList.toMutableList()
            tasks[getSelectedItemPositions().first()] = task.copy(title = title)
            saveTasks(tasks)
            finishActMode()
        }
    }

    private fun deleteSelection() {
        val tasks = currentList.toMutableList()
        val tasksToRemove = ArrayList<Task>(selectedKeys.size)
        selectedKeys.forEach { key ->
            val position = tasks.indexOfFirst { it.id == key }
            if (position != -1) {
                val favorite = getItemWithKey(key)
                if (favorite != null) {
                    tasksToRemove.add(favorite)
                }
            }
        }

        tasks.removeAll(tasksToRemove.toSet())
        saveTasks(tasks)
    }

    private fun moveSelectedItemsToTop() {
        activity.config.sorting = SORT_BY_CUSTOM
        val tasks = currentList.toMutableList()
        selectedKeys.reversed().forEach { id ->
            val position = tasks.indexOfFirst { it.id == id }
            val tempItem = tasks[position]
            tasks.removeAt(position)
            tasks.add(0, tempItem)
        }

        saveTasks(tasks)
    }

    private fun moveSelectedItemsToBottom() {
        activity.config.sorting = SORT_BY_CUSTOM
        val tasks = currentList.toMutableList()
        selectedKeys.forEach { id ->
            val position = tasks.indexOfFirst { it.id == id }
            val tempItem = tasks[position]
            tasks.removeAt(position)
            tasks.add(tasks.size, tempItem)
        }

        saveTasks(tasks)
    }

    private fun getItemWithKey(key: Int): Task? = currentList.firstOrNull { it.id == key }

    private fun getSelectedItems() = currentList.filter { selectedKeys.contains(it.id) }.toMutableList()

    private fun setupView(view: View, task: Task, holder: ViewHolder) {
        val isSelected = selectedKeys.contains(task.id)
        ItemChecklistBinding.bind(view).apply {
            checklistTitle.apply {
                text = task.title
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getPercentageFontSize())
                gravity = context.config.getTextGravity()

                if (task.isDone) {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    alpha = DONE_CHECKLIST_ITEM_ALPHA
                } else {
                    paintFlags = paintFlags.removeBit(Paint.STRIKE_THRU_TEXT_FLAG)
                    alpha = 1f
                }
            }

            checklistCheckbox.isChecked = task.isDone
            checklistHolder.isSelected = isSelected

            checklistDragHandle.beVisibleIf(selectedKeys.isNotEmpty())
            checklistDragHandle.applyColorFilter(textColor)
            checklistDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        activity.config.sorting = SORT_BY_CUSTOM
        val tasks = currentList.toMutableList()
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(tasks, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(tasks, i, i - 1)
            }
        }

        saveTasks(tasks)
    }

    override fun onRowSelected(myViewHolder: MyRecyclerViewAdapter.ViewHolder?) {}

    override fun onRowClear(myViewHolder: MyRecyclerViewAdapter.ViewHolder?) {
        saveTasks(currentList.toList())
    }

    private fun saveTasks(tasks: List<Task>) {
        listener?.saveTasks(tasks) {
            listener.refreshItems()
        }
    }
}

private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(
        oldItem: Task,
        newItem: Task
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: Task,
        newItem: Task
    ) = oldItem.id == newItem.id
        && oldItem.isDone == newItem.isDone
        && oldItem.title == newItem.title
        && oldItem.dateCreated == newItem.dateCreated
}
