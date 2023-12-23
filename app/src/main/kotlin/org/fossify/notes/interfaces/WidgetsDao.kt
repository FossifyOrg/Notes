package org.fossify.notes.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.notes.models.Widget

@Dao
interface WidgetsDao {
    @Query("SELECT * FROM widgets")
    fun getWidgets(): List<Widget>

    @Query("SELECT * FROM widgets WHERE widget_id = :widgetId")
    fun getWidgetWithWidgetId(widgetId: Int): Widget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(widget: Widget): Long

    @Query("DELETE FROM widgets WHERE note_id = :noteId")
    fun deleteNoteWidgets(noteId: Long)

    @Query("DELETE FROM widgets WHERE widget_id = :widgetId")
    fun deleteWidgetId(widgetId: Int)
}
