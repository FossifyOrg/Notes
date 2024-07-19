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
