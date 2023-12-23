package org.fossify.notes.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.notes.R
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.databinding.DialogExportFileBinding
import org.fossify.notes.extensions.config
import org.fossify.notes.models.Note
import java.io.File

class ExportFileDialog(val activity: SimpleActivity, val note: Note, val callback: (exportPath: String) -> Unit) {

    init {
        var realPath = File(note.path).parent ?: activity.config.lastUsedSavePath
        val binding = DialogExportFileBinding.inflate(activity.layoutInflater).apply {
            filePath.setText(activity.humanizePath(realPath))

            fileName.setText(note.title)
            extension.setText(activity.config.lastUsedExtension)
            filePath.setOnClickListener {
                FilePickerDialog(activity, realPath, false, false, true, true) {
                    filePath.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_as_file) { alertDialog ->
                    alertDialog.showKeyboard(binding.fileName)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.fileName.value
                        val extension = binding.extension.value

                        if (filename.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val fullFilename = if (extension.isEmpty()) filename else "$filename.$extension"
                        if (!fullFilename.isAValidFilename()) {
                            activity.toast(
                                String.format(
                                    activity.getString(
                                        org.fossify.commons.R.string.filename_invalid_characters_placeholder,
                                        fullFilename
                                    )
                                )
                            )
                            return@setOnClickListener
                        }

                        activity.config.lastUsedExtension = extension
                        activity.config.lastUsedSavePath = realPath
                        callback("$realPath/$fullFilename")
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
