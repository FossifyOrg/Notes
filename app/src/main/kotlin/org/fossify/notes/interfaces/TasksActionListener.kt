package org.fossify.notes.interfaces

import org.fossify.notes.models.Task

interface TasksActionListener {
    fun refreshItems()

    fun saveTasks(updatedTasks: List<Task>, callback: () -> Unit = {})
}
