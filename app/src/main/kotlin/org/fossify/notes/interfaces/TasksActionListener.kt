package org.fossify.notes.interfaces

import org.fossify.notes.models.Task

interface TasksActionListener {
    fun editTask(task: Task, callback: () -> Unit)

    fun deleteTasks(tasksToDelete: List<Task>)

    fun moveTask(fromPosition: Int, toPosition: Int)

    fun moveTasksToTop(taskIds: List<Int>)

    fun moveTasksToBottom(taskIds: List<Int>)

    fun saveAndReload()
}
