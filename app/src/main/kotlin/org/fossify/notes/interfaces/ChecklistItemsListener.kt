package org.fossify.notes.interfaces

import org.fossify.notes.models.ChecklistItem

interface ChecklistItemsListener {
    fun refreshItems()

    fun saveChecklist(updatedItems: List<ChecklistItem>, callback: () -> Unit = {})
}
