package org.fossify.notes.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import org.fossify.notes.R
import org.fossify.notes.databinding.DialogNewChecklistItemBinding
import org.fossify.notes.databinding.ItemAddChecklistBinding
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.R as CommonsR

class ChecklistItemDialogFragment : DialogFragment() {

    private val activeInputFields = mutableListOf<AppCompatEditText>()
    private var binding: DialogNewChecklistItemBinding? = null

    companion object {
        const val DIALOG_TAG = "ChecklistItemDialogFragment"
        const val REQUEST_KEY = "ChecklistItemRequest"
        const val RESULT_TEXT_KEY = "ResultText"
        const val RESULT_TASK_ID_KEY = "ResultTaskId"

        private const val ARG_TEXT = "ArgText"
        private const val ARG_TASK_ID = "ArgTaskId"
        private const val SAVED_STATE_TEXTS = "SavedStateTexts"

        fun newInstance(taskId: Int = -1, text: String = ""): ChecklistItemDialogFragment {
            val fragment = ChecklistItemDialogFragment()
            val args = Bundle()
            args.putInt(ARG_TASK_ID, taskId)
            args.putString(ARG_TEXT, text)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val taskId = arguments?.getInt(ARG_TASK_ID) ?: -1

        binding = DialogNewChecklistItemBinding.inflate(activity.layoutInflater)

        activeInputFields.clear()

        // Restore rows
        if (savedInstanceState != null) {
            val savedTexts = savedInstanceState.getStringArrayList(SAVED_STATE_TEXTS)
            if (!savedTexts.isNullOrEmpty()) {
                savedTexts.forEach { text -> addNewRow(text) }
            } else {
                addNewRow("")
            }
        } else {
            val initialText = arguments?.getString(ARG_TEXT) ?: ""
            addNewRow(initialText)
        }

        val isNewTaskMode = (taskId == -1)
        if (isNewTaskMode) {
            val contrastColor = activity.getProperPrimaryColor().getContrastColor()
            binding!!.addItem.setColorFilter(contrastColor)

            binding!!.addItem.setOnClickListener {
                addNewRow("")
            }
        } else {
            binding!!.addItem.visibility = View.GONE
            binding!!.settingsAddChecklistTop.visibility = View.GONE
        }

        val titleRes = if (isNewTaskMode) R.string.add_new_checklist_items else R.string.rename_note

        val builder = activity.getAlertDialogBuilder()
            .setTitle(titleRes)
            .setView(binding!!.root)
            .setPositiveButton(CommonsR.string.ok, null)
            .setNegativeButton(CommonsR.string.cancel, null)

        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialog.setOnShowListener {
            if (activeInputFields.isNotEmpty()) {
                dialog.showKeyboard(activeInputFields.last())
            }

            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val combinedText = activeInputFields
                    .map { it.text.toString().trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                if (combinedText.isNotEmpty()) {
                    val resultBundle = Bundle().apply {
                        putString(RESULT_TEXT_KEY, combinedText)
                        putInt(RESULT_TASK_ID_KEY, taskId)
                    }
                    parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun addNewRow(text: String) {
        val rowBinding = ItemAddChecklistBinding.inflate(layoutInflater)

        // We disable automatic state saving for this view.
        // This prevents Android from confusing the multiple EditTexts (which all share the same ID)
        // and overwriting our manually restored text with the last view's text.
        rowBinding.titleEditText.isSaveEnabled = false

        rowBinding.titleEditText.setText(text)

        if (text.isNotEmpty()) {
            rowBinding.titleEditText.setSelection(text.length)
        }

        val inputField = rowBinding.titleEditText as AppCompatEditText
        activeInputFields.add(inputField)

        binding?.checklistHolder?.addView(rowBinding.root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentTexts = ArrayList(activeInputFields.map { it.text.toString() })
        outState.putStringArrayList(SAVED_STATE_TEXTS, currentTexts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
