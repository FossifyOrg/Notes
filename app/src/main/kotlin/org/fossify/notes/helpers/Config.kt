package org.fossify.notes.helpers

import android.content.Context
import android.os.Environment
import android.view.Gravity
import org.fossify.commons.helpers.BaseConfig
import org.fossify.notes.models.NoteType

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var autosaveNotes: Boolean
        get() = prefs.getBoolean(AUTOSAVE_NOTES, true)
        set(autosaveNotes) = prefs.edit().putBoolean(AUTOSAVE_NOTES, autosaveNotes).apply()

    var displaySuccess: Boolean
        get() = prefs.getBoolean(DISPLAY_SUCCESS, false)
        set(displaySuccess) = prefs.edit().putBoolean(DISPLAY_SUCCESS, displaySuccess).apply()

    var clickableLinks: Boolean
        get() = prefs.getBoolean(CLICKABLE_LINKS, false)
        set(clickableLinks) = prefs.edit().putBoolean(CLICKABLE_LINKS, clickableLinks).apply()

    var monospacedFont: Boolean
        get() = prefs.getBoolean(MONOSPACED_FONT, false)
        set(monospacedFont) = prefs.edit().putBoolean(MONOSPACED_FONT, monospacedFont).apply()

    var showKeyboard: Boolean
        get() = prefs.getBoolean(SHOW_KEYBOARD, true)
        set(showKeyboard) = prefs.edit().putBoolean(SHOW_KEYBOARD, showKeyboard).apply()

    var showNotePicker: Boolean
        get() = prefs.getBoolean(SHOW_NOTE_PICKER, false)
        set(showNotePicker) = prefs.edit().putBoolean(SHOW_NOTE_PICKER, showNotePicker).apply()

    var showWordCount: Boolean
        get() = prefs.getBoolean(SHOW_WORD_COUNT, false)
        set(showWordCount) = prefs.edit().putBoolean(SHOW_WORD_COUNT, showWordCount).apply()

    var gravity: Int
        get() = prefs.getInt(GRAVITY, GRAVITY_START)
        set(size) = prefs.edit().putInt(GRAVITY, size).apply()

    var currentNoteId: Long
        get() = prefs.getLong(CURRENT_NOTE_ID, 1L)
        set(id) = prefs.edit().putLong(CURRENT_NOTE_ID, id).apply()

    var widgetNoteId: Long
        get() = prefs.getLong(WIDGET_NOTE_ID, 1L)
        set(id) = prefs.edit().putLong(WIDGET_NOTE_ID, id).apply()

    var placeCursorToEnd: Boolean
        get() = prefs.getBoolean(CURSOR_PLACEMENT, true)
        set(placement) = prefs.edit().putBoolean(CURSOR_PLACEMENT, placement).apply()

    var enableLineWrap: Boolean
        get() = prefs.getBoolean(ENABLE_LINE_WRAP, true)
        set(enableLineWrap) = prefs.edit().putBoolean(ENABLE_LINE_WRAP, enableLineWrap).apply()

    var lastUsedExtension: String
        get() = prefs.getString(LAST_USED_EXTENSION, "txt")!!
        set(lastUsedExtension) = prefs.edit().putString(LAST_USED_EXTENSION, lastUsedExtension).apply()

    var lastUsedSavePath: String
        get() = prefs.getString(LAST_USED_SAVE_PATH, Environment.getExternalStorageDirectory().toString())!!
        set(lastUsedSavePath) = prefs.edit().putString(LAST_USED_SAVE_PATH, lastUsedSavePath).apply()

    var useIncognitoMode: Boolean
        get() = prefs.getBoolean(USE_INCOGNITO_MODE, false)
        set(useIncognitoMode) = prefs.edit().putBoolean(USE_INCOGNITO_MODE, useIncognitoMode).apply()

    var lastCreatedNoteType: Int
        get() = prefs.getInt(LAST_CREATED_NOTE_TYPE, NoteType.TYPE_TEXT.value)
        set(lastCreatedNoteType) = prefs.edit().putInt(LAST_CREATED_NOTE_TYPE, lastCreatedNoteType).apply()

    fun getTextGravity() = when (gravity) {
        GRAVITY_CENTER -> Gravity.CENTER_HORIZONTAL
        GRAVITY_END -> Gravity.END
        else -> Gravity.START
    }

    var fontSizePercentage: Int
        get() = prefs.getInt(FONT_SIZE_PERCENTAGE, 100)
        set(fontSizePercentage) = prefs.edit().putInt(FONT_SIZE_PERCENTAGE, fontSizePercentage).apply()

    var addNewChecklistItemsTop: Boolean
        get() = prefs.getBoolean(ADD_NEW_CHECKLIST_ITEMS_TOP, false)
        set(addNewCheckListItemsTop) = prefs.edit().putBoolean(ADD_NEW_CHECKLIST_ITEMS_TOP, addNewCheckListItemsTop).apply()

    fun getSorting(noteId: Long?) = if (noteId == null) sorting else getFolderSorting(noteId.toString())

    fun hasOwnSorting(noteId: Long?) = noteId != null && hasCustomSorting(noteId.toString())

    fun saveOwnSorting(noteId: Long, sorting: Int) = saveCustomSorting(noteId.toString(), sorting)

    fun removeOwnSorting(noteId: Long) = removeCustomSorting(noteId.toString())

    fun getMoveDoneChecklistItems(noteId: Long?): Boolean = getSorting(noteId) and SORT_MOVE_DONE_ITEMS != 0

    fun migrateMoveDoneChecklistItems() {
        val isMigrated = prefs.getBoolean(MIGRATED_MOVE_DONE_CHECKLIST_ITEMS, false)
        if (isMigrated) {
            return
        }
        val oldValue = prefs.getBoolean(MOVE_DONE_CHECKLIST_ITEMS, true)
        if (oldValue) {
            sorting = sorting or SORT_MOVE_DONE_ITEMS
        }
        prefs.edit().putBoolean(MIGRATED_MOVE_DONE_CHECKLIST_ITEMS, true).apply()
    }
}
