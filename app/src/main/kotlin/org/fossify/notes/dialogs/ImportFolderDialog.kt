package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.PROTECTION_NONE
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogImportFolderBinding
import org.fossify.notes.extensions.notesDB
import org.fossify.notes.extensions.parseChecklistItems
import org.fossify.notes.helpers.NotesHelper
import org.fossify.notes.models.Note
import org.fossify.notes.models.NoteType
import java.io.File

class ImportFolderDialog(val activity: SimpleActivity, val path: String, val callback: () -> Unit) : AlertDialog.Builder(activity) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogImportFolderBinding.inflate(activity.layoutInflater).apply {
            openFileFilename.setText(activity.humanizePath(path))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.import_folder) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val updateFilesOnEdit = binding.openFileType.checkedRadioButtonId == R.id.open_file_update_file
                        ensureBackgroundThread {
                            saveFolder(updateFilesOnEdit)
                        }
                    }
                }
            }
    }

    private fun saveFolder(updateFilesOnEdit: Boolean) {
        val folder = File(path)
        folder.listFiles { file ->
            val filename = file.path.getFilenameFromPath()
            when {
                file.isDirectory -> false
                filename.isMediaFile() -> false
                file.length() > 1000 * 1000 -> false
                activity.notesDB.getNoteIdWithTitle(filename) != null -> false
                else -> true
            }
        }?.forEach {
            val storePath = if (updateFilesOnEdit) it.absolutePath else ""
            val title = it.absolutePath.getFilenameFromPath()
            val value = if (updateFilesOnEdit) "" else it.readText()
            val fileText = it.readText().trim()
            val checklistItems = fileText.parseChecklistItems()
            if (checklistItems != null) {
                saveNote(title.substringBeforeLast('.'), fileText, NoteType.TYPE_CHECKLIST, "")
            } else {
                if (updateFilesOnEdit) {
                    activity.handleSAFDialog(path) {
                        saveNote(title, value, NoteType.TYPE_TEXT, storePath)
                    }
                } else {
                    saveNote(title, value, NoteType.TYPE_TEXT, storePath)
                }
            }
        }

        activity.runOnUiThread {
            callback()
            dialog?.dismiss()
        }
    }

    private fun saveNote(title: String, value: String, type: NoteType, path: String) {
        val note = Note(null, title, value, type, path, PROTECTION_NONE, "")
        NotesHelper(activity).insertOrUpdateNote(note)
    }
}
