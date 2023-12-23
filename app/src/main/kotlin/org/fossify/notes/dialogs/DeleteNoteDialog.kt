package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogDeleteNoteBinding
import org.fossify.notes.models.Note

class DeleteNoteDialog(val activity: SimpleActivity, val note: Note, val callback: (deleteFile: Boolean) -> Unit) {
    var dialog: AlertDialog? = null

    init {
        val message = String.format(activity.getString(R.string.delete_note_prompt_message), note.title)
        val binding = DialogDeleteNoteBinding.inflate(activity.layoutInflater).apply {
            if (note.path.isNotEmpty()) {
                deleteNoteCheckbox.text = String.format(activity.getString(R.string.delete_file_itself), note.path)
                deleteNoteCheckboxHolder.beVisible()
                deleteNoteCheckboxHolder.setOnClickListener {
                    deleteNoteCheckbox.toggle()
                }
            }
            deleteNoteDescription.text = message
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.delete) { dialog, which -> dialogConfirmed(binding.deleteNoteCheckbox.isChecked) }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed(deleteFile: Boolean) {
        callback(deleteFile && note.path.isNotEmpty())
        dialog?.dismiss()
    }
}
