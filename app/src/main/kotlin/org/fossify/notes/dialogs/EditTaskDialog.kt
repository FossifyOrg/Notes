package org.fossify.notes.dialogs

import android.app.Activity
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.inputmethod.EditorInfo
import org.fossify.commons.extensions.*
import org.fossify.notes.databinding.DialogRenameChecklistItemBinding
import org.fossify.notes.extensions.config

class EditTaskDialog(val activity: Activity, val oldTitle: String, callback: (newTitle: String) -> Unit) {
    init {
        val binding = DialogRenameChecklistItemBinding.inflate(activity.layoutInflater).apply {
            checklistItemTitle.setText(oldTitle)

            if (activity.config.useIncognitoMode == true) {
                checklistItemTitle.imeOptions =
                    checklistItemTitle.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            } else {
                checklistItemTitle.imeOptions =
                    checklistItemTitle.imeOptions.removeBit(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.checklistItemTitle)
                    alertDialog.getButton(BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.checklistItemTitle.value
                        when {
                            newTitle.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            else -> {
                                callback(newTitle)
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
