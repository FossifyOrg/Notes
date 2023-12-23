package org.fossify.notes.dialogs

import android.app.Activity
import android.content.DialogInterface.BUTTON_POSITIVE
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.PROTECTION_NONE
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.notes.R
import org.fossify.notes.databinding.DialogNewNoteBinding
import org.fossify.notes.extensions.config
import org.fossify.notes.extensions.notesDB
import org.fossify.notes.models.Note
import org.fossify.notes.models.NoteType

class NewNoteDialog(val activity: Activity, title: String? = null, val setChecklistAsDefault: Boolean, callback: (note: Note) -> Unit) {
    init {
        val binding = DialogNewNoteBinding.inflate(activity.layoutInflater).apply {
            val defaultType = when {
                setChecklistAsDefault -> typeChecklist.id
                activity.config.lastCreatedNoteType == NoteType.TYPE_TEXT.value -> typeTextNote.id
                else -> typeChecklist.id
            }

            newNoteType.check(defaultType)
        }

        binding.lockedNoteTitle.setText(title)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.new_note) { alertDialog ->
                    alertDialog.showKeyboard(binding.lockedNoteTitle)
                    alertDialog.getButton(BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.lockedNoteTitle.value
                        ensureBackgroundThread {
                            when {
                                newTitle.isEmpty() -> activity.toast(R.string.no_title)
                                activity.notesDB.getNoteIdWithTitle(newTitle) != null -> activity.toast(R.string.title_taken)
                                else -> {
                                    val type = if (binding.newNoteType.checkedRadioButtonId == binding.typeChecklist.id) {
                                        NoteType.TYPE_CHECKLIST
                                    } else {
                                        NoteType.TYPE_TEXT
                                    }

                                    activity.config.lastCreatedNoteType = type.value
                                    val newNote = Note(null, newTitle, "", type, "", PROTECTION_NONE, "")
                                    callback(newNote)
                                    alertDialog.dismiss()
                                }
                            }
                        }
                    }
                }
            }
    }
}
