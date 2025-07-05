package org.fossify.notes.dialogs

import android.app.Activity
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.DARK_GREY
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.notes.R
import org.fossify.notes.databinding.DialogNewChecklistItemBinding
import org.fossify.notes.databinding.ItemAddChecklistBinding
import org.fossify.notes.extensions.config

class NewChecklistItemDialog(
    val activity: Activity,
    private val noteId: Long,
    callback: (titles: ArrayList<String>) -> Unit
) {
    private val titles = mutableListOf<AppCompatEditText>()
    private val binding = DialogNewChecklistItemBinding.inflate(activity.layoutInflater)
    private val view = binding.root

    init {
        addNewEditText()
        val plusTextColor = if (activity.isWhiteTheme()) {
            DARK_GREY
        } else {
            activity.getProperPrimaryColor().getContrastColor()
        }

        binding.apply {
            addItem.applyColorFilter(plusTextColor)
            addItem.setOnClickListener {
                addNewEditText()
            }
            settingsAddChecklistTop.beVisibleIf(activity.config.getSorting(noteId) == SORT_BY_CUSTOM)
            settingsAddChecklistTop.isChecked = activity.config.addNewChecklistItemsTop
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.add_new_checklist_items) { alertDialog ->
                    alertDialog.showKeyboard(titles.first())
                    alertDialog.getButton(BUTTON_POSITIVE).setOnClickListener {
                        activity.config.addNewChecklistItemsTop = binding.settingsAddChecklistTop.isChecked
                        when {
                            titles.all { it.text!!.isEmpty() } -> activity.toast(org.fossify.commons.R.string.empty_name)
                            else -> {
                                val titles = titles.map { it.text.toString() }.filter { it.isNotEmpty() }.toMutableList() as ArrayList<String>
                                callback(titles)
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }

    private fun addNewEditText(initialText: String? = null, position: Int? = null) {
        ItemAddChecklistBinding.inflate(activity.layoutInflater).apply {
            titleEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.KEYCODE_ENTER) {
                    addNewEditText(position = titles.indexOf(titleEditText) + 1)
                    true
                } else {
                    false
                }
            }
            titleEditText.onTextChangeListener { text ->
                val lines = text.lines().filter { it.trim().isNotEmpty() }
                if (lines.size > 1) {
                    val currentPosition = titles.indexOf(titleEditText)
                    lines.forEachIndexed { i, line ->
                        if (i == 0) {
                            titleEditText.setText(line)
                        } else {
                            addNewEditText(line, currentPosition + i)
                        }
                    }
                }
            }
            if (initialText != null) {
                titleEditText.append(initialText)
            }
            if (position != null && position < titles.size) {
                titles.add(position, titleEditText)
                binding.checklistHolder.addView(this.root, position)
            } else {
                titles.add(titleEditText)
                binding.checklistHolder.addView(this.root)
            }

            if (activity.config.useIncognitoMode == true) {
                titleEditText.imeOptions =
                    titleEditText.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            } else {
                titleEditText.imeOptions =
                    titleEditText.imeOptions.removeBit(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
            }

            activity.updateTextColors(binding.checklistHolder)
            binding.dialogHolder.post {
                binding.dialogHolder.fullScroll(View.FOCUS_DOWN)
                activity.showKeyboard(titleEditText)
            }
        }
    }
}
