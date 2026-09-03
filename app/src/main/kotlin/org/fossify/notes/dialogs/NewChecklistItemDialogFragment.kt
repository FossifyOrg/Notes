package org.fossify.notes.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.notes.R
import org.fossify.notes.databinding.DialogNewChecklistItemBinding
import org.fossify.notes.databinding.ItemAddChecklistBinding
import org.fossify.notes.extensions.config
import org.fossify.notes.extensions.maybeRequestIncognito

class NewChecklistItemDialogFragment : DialogFragment() {

    private val activeInputFields = mutableListOf<AppCompatEditText>()
    private var binding: DialogNewChecklistItemBinding? = null

    // Track the index of the currently focused row
    private var lastFocusedIndex = -1

    companion object {
        const val TAG = "NewChecklistItemDialogFragment"
        const val REQUEST_KEY = "new_checklist_item_request"
        const val RESULT_TEXT = "result_text"
        const val RESULT_ADD_TOP = "result_add_top"

        private const val ARG_NOTE_ID = "arg_note_id"
        private const val STATE_TEXTS = "state_texts"

        private const val STATE_FOCUSED_INDEX = "state_focused_index"


        fun show(
            host: androidx.fragment.app.FragmentManager,
            noteId: Long
        ) = NewChecklistItemDialogFragment().apply {
            arguments = bundleOf(ARG_NOTE_ID to noteId)
        }.show(host, TAG)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val activity = requireActivity()
        binding = DialogNewChecklistItemBinding.inflate(activity.layoutInflater)
        activeInputFields.clear()

        // Restore state or add initial row
        if (savedInstanceState != null) {
            val savedTexts = savedInstanceState.getStringArrayList(STATE_TEXTS)
            if (!savedTexts.isNullOrEmpty()) {
                savedTexts.forEach { text -> addNewRow(text) }
            } else {
                addNewRow("")
            }
            // Restore the focus index
            lastFocusedIndex = savedInstanceState.getInt(STATE_FOCUSED_INDEX, -1)
        } else {
            addNewRow("")
        }

        // Setup UI
        val noteId = requireArguments().getLong(ARG_NOTE_ID)
        val contrastColor = activity.getProperPrimaryColor().getContrastColor()
        binding!!.addItem.setColorFilter(contrastColor)

        // Insert after the currently focused row
        binding!!.addItem.setOnClickListener {
            val insertIndex = if (lastFocusedIndex != -1 && lastFocusedIndex < activeInputFields.size) {
                lastFocusedIndex + 1
            } else {
                null // Append to end if nothing is focused
            }
            addNewRow("", focus = true, position = insertIndex)
        }

        val config = activity.config
        binding!!.settingsAddChecklistTop.beVisibleIf(config.getSorting(noteId) == SORT_BY_CUSTOM)
        binding!!.settingsAddChecklistTop.isChecked = config.addNewChecklistItemsTop

        val builder = activity.getAlertDialogBuilder()
            .setTitle(R.string.add_new_checklist_items)
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)

        var dialog: AlertDialog? = null
        activity.setupDialogStuff(binding!!.root, builder) { alert ->

            // Apply Focus : if we have a valid restored index, use it
            if (lastFocusedIndex != -1 && lastFocusedIndex < activeInputFields.size) {
                alert.showKeyboard(activeInputFields[lastFocusedIndex])
            } else if (activeInputFields.isNotEmpty()) {
                // Default to the last
                alert.showKeyboard(activeInputFields.last())
            }

            alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                // Collect all texts
                val combinedText = activeInputFields
                    .map { it.text.toString().trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                if (combinedText.isEmpty()) {
                    activity.toast(org.fossify.commons.R.string.empty_name)
                } else {
                    config.addNewChecklistItemsTop = binding!!.settingsAddChecklistTop.isChecked

                    // Return result
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            RESULT_TEXT to combinedText,
                            RESULT_ADD_TOP to binding!!.settingsAddChecklistTop.isChecked
                        )
                    )
                    alert.dismiss()
                }
            }
            dialog = alert
        }

        return dialog!!
    }

    private fun addNewRow(text: String, focus: Boolean = false, position: Int? = null) {
        val rowBinding = ItemAddChecklistBinding.inflate(layoutInflater)

        // Disable state saving for individual views to avoid rotation conflict
        rowBinding.titleEditText.isSaveEnabled = false
        rowBinding.titleEditText.setText(text)
        rowBinding.titleEditText.maybeRequestIncognito()

        if (text.isNotEmpty()) {
            rowBinding.titleEditText.setSelection(text.length)
        }

        // Track focus changes in real time
        rowBinding.titleEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // When this view gets focus, remember its index
                lastFocusedIndex = activeInputFields.indexOf(view)
            }
        }

        // Add "Enter" key listener to create new rows automatically
        rowBinding.titleEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {

                val currentIndex = activeInputFields.indexOf(v)
                addNewRow("", focus = true, position = currentIndex + 1)
                true
            } else {
                false
            }
        }

        val inputField = rowBinding.titleEditText as AppCompatEditText

        // Insert into list and view hierarchy at correct position
        if (position != null && position < activeInputFields.size) {
            activeInputFields.add(position, inputField)
            binding?.checklistHolder?.addView(rowBinding.root, position)
        } else {
            activeInputFields.add(inputField)
            binding?.checklistHolder?.addView(rowBinding.root)
        }


        if (focus) {
            binding?.dialogHolder?.post {
                // Only scroll to bottom if appending to the end
                if (position == null) {
                    binding?.dialogHolder?.fullScroll(View.FOCUS_DOWN)
                }

                inputField.requestFocus()
                requireActivity().showKeyboard(inputField)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentTexts = ArrayList(activeInputFields.map { it.text.toString() })
        outState.putStringArrayList(STATE_TEXTS, currentTexts)

        // Save the index tracked via the listener
        outState.putInt(STATE_FOCUSED_INDEX, lastFocusedIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        activeInputFields.clear()
    }
}
