package org.fossify.notes.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.fossify.notes.models.Task

fun String.parseChecklistItems(): ArrayList<Task>? {
    if (startsWith("[{") && endsWith("}]")) {
        try {
            val taskType = object : TypeToken<List<Task>>() {}.type
            return Gson().fromJson<ArrayList<Task>>(this, taskType) ?: null
        } catch (e: Exception) {
        }
    }
    return null
}

fun String.checklistToPlainText(moveDoneToBottom: Boolean = true): String? {
    val tasks = parseChecklistItems() ?: return null
    val sortedTasks = if (moveDoneToBottom) tasks.sortedBy { it.isDone } else tasks
    return sortedTasks.joinToString("\n") { task ->
        val checkbox = if (task.isDone) "[x]" else "[ ]"
        "$checkbox ${task.title}"
    }
}
