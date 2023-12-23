package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogExportFilesBinding
import org.fossify.notes.extensions.config
import org.fossify.notes.models.Note

class ExportFilesDialog(val activity: SimpleActivity, val notes: ArrayList<Note>, val callback: (parent: String, extension: String) -> Unit) {
    init {
        var realPath = activity.config.lastUsedSavePath
        val binding = DialogExportFilesBinding.inflate(activity.layoutInflater).apply {
            folderPath.setText(activity.humanizePath(realPath))

            extension.setText(activity.config.lastUsedExtension)
            folderPath.setOnClickListener {
                FilePickerDialog(activity, realPath, false, false, true, true) {
                    folderPath.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_as_file) { alertDialog ->
                    alertDialog.showKeyboard(binding.extension)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        activity.handleSAFDialog(realPath) {
                            val extension = binding.extension.value
                            activity.config.lastUsedExtension = extension
                            activity.config.lastUsedSavePath = realPath
                            callback(realPath, extension)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
