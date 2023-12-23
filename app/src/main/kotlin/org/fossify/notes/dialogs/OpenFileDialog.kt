package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.humanizePath
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.PROTECTION_NONE
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogOpenFileBinding
import org.fossify.notes.models.Note
import org.fossify.notes.models.NoteType
import java.io.File

class OpenFileDialog(val activity: SimpleActivity, val path: String, val callback: (note: Note) -> Unit) : AlertDialog.Builder(activity) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogOpenFileBinding.inflate(activity.layoutInflater).apply {
            openFileFilename.setText(activity.humanizePath(path))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.open_file) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val updateFileOnEdit = binding.openFileType.checkedRadioButtonId == binding.openFileUpdateFile.id
                        val storePath = if (updateFileOnEdit) path else ""
                        val storeContent = if (updateFileOnEdit) "" else File(path).readText()

                        if (updateFileOnEdit) {
                            activity.handleSAFDialog(path) {
                                saveNote(storeContent, storePath)
                            }
                        } else {
                            saveNote(storeContent, storePath)
                        }
                    }
                }
            }
    }

    private fun saveNote(storeContent: String, storePath: String) {
        val filename = path.getFilenameFromPath()
        val note = Note(null, filename, storeContent, NoteType.TYPE_TEXT, storePath, PROTECTION_NONE, "")
        callback(note)
        dialog?.dismiss()
    }
}
