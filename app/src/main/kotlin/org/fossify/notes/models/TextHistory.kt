package org.fossify.notes.models

import java.util.LinkedList

class TextHistory {
    var position = 0
    val history = LinkedList<TextHistoryItem>()

    fun getPrevious(): TextHistoryItem? {
        if (position == 0) {
            return null
        }
        position--
        return history[position]
    }

    fun getNext(): TextHistoryItem? {
        if (position >= history.size) {
            return null
        }

        val item = history[position]
        position++
        return item
    }

    fun add(item: TextHistoryItem) {
        while (history.size > position) {
            history.removeAt(history.lastIndex)
        }

        history.add(item)
        position++
    }
}
