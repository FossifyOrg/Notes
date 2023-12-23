package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.views.AutoStaggeredGridLayoutManager
import org.fossify.notes.R
import org.fossify.notes.adapters.OpenNoteAdapter
import org.fossify.notes.databinding.DialogOpenNoteBinding
import org.fossify.notes.helpers.NotesHelper
import org.fossify.notes.models.Note

class OpenNoteDialog(val activity: BaseSimpleActivity, val callback: (checkedId: Long, newNote: Note?) -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogOpenNoteBinding.inflate(activity.layoutInflater)

        val noteItemWidth = activity.resources.getDimensionPixelSize(R.dimen.grid_note_item_width)
        binding.dialogOpenNoteList.layoutManager = AutoStaggeredGridLayoutManager(noteItemWidth, StaggeredGridLayoutManager.VERTICAL)

        NotesHelper(activity).getNotes {
            initDialog(it, binding)
        }
    }

    private fun initDialog(notes: List<Note>, binding: DialogOpenNoteBinding) {
        binding.dialogOpenNoteList.adapter = OpenNoteAdapter(activity, notes, binding.dialogOpenNoteList) {
            it as Note
            callback(it.id!!, null)
            dialog?.dismiss()
        }

        binding.newNoteFab.setOnClickListener {
            NewNoteDialog(activity, setChecklistAsDefault = false) {
                callback(0, it)
                dialog?.dismiss()
            }
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.open_note) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }
}
