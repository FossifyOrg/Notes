package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogExportNotesBinding

class ExportNotesDialog(val activity: SimpleActivity, callback: (filename: String) -> Unit) {

    init {
        val binding = DialogExportNotesBinding.inflate(activity.layoutInflater).apply {
            exportNotesFilename.setText(
                buildString {
                    append(root.context.getString(org.fossify.commons.R.string.notes))
                    append("_")
                    append(root.context.getCurrentFormattedDateTime())
                }
            )
        }

        activity.getAlertDialogBuilder().setPositiveButton(org.fossify.commons.R.string.ok, null).setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_notes) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                        val filename = binding.exportNotesFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                callback(filename)
                                alertDialog.dismiss()
                            }

                            else -> activity.toast(org.fossify.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }
}

