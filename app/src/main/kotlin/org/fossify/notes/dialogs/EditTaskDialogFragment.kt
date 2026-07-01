package org.fossify.notes.dialogs

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.notes.databinding.DialogRenameChecklistItemBinding
import org.fossify.notes.extensions.maybeRequestIncognito
import org.fossify.notes.models.Task

class EditTaskDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EditTaskDialog"
        const val ARG_TASK_ID = "arg_task_id"
        const val ARG_OLD_TITLE = "arg_old_title"
        const val REQUEST_KEY = "edit_task_request"
        const val RESULT_TITLE = "result_title"
        const val RESULT_TASK_ID = "result_task_id"
        private const val STATE_TEXT = "state_text"

        fun show(
            host: androidx.fragment.app.FragmentManager,
            task: Task
        ) = EditTaskDialogFragment().apply {
            arguments = bundleOf(ARG_OLD_TITLE to task.title, ARG_TASK_ID to task.id)
        }.show(host, TAG)
    }

    private lateinit var binding: DialogRenameChecklistItemBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val activity = requireActivity()
        binding = DialogRenameChecklistItemBinding.inflate(activity.layoutInflater).also {
            val restored = savedInstanceState?.getString(STATE_TEXT)
            it.checklistItemTitle.setText(
                restored ?: requireArguments().getString(ARG_OLD_TITLE).orEmpty()
            )
            it.checklistItemTitle.maybeRequestIncognito()
        }

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)

        var dialog: AlertDialog? = null
        activity.setupDialogStuff(binding.root, builder) { alert ->
            alert.showKeyboard(binding.checklistItemTitle)
            alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val newTitle = binding.checklistItemTitle.text?.toString().orEmpty()
                if (newTitle.isEmpty()) {
                    activity.toast(org.fossify.commons.R.string.empty_name)
                } else {
                    val taskId = requireArguments().getInt(ARG_TASK_ID)
                    parentFragmentManager
                        .setFragmentResult(
                            REQUEST_KEY, bundleOf(RESULT_TASK_ID to taskId, RESULT_TITLE to newTitle)
                        )
                    alert.dismiss()
                }
            }
            dialog = alert
        }

        return dialog!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val text = binding.checklistItemTitle.text?.toString().orEmpty()
        outState.putString(STATE_TEXT, text)
        super.onSaveInstanceState(outState)
    }
}
