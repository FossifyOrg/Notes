package org.fossify.notes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.notes.activities.SimpleActivity
import org.fossify.notes.adapters.TasksAdapter
import org.fossify.notes.databinding.FragmentChecklistBinding
import org.fossify.notes.dialogs.NewChecklistItemDialog
import org.fossify.notes.extensions.config
import org.fossify.notes.extensions.updateWidgets
import org.fossify.notes.helpers.NOTE_ID
import org.fossify.notes.helpers.NotesHelper
import org.fossify.notes.interfaces.TasksActionListener
import org.fossify.notes.models.Note
import org.fossify.notes.models.Task
import java.io.File

class TasksFragment : NoteFragment(), TasksActionListener {

    private var noteId = 0L

    private lateinit var binding: FragmentChecklistBinding

    var tasks = mutableListOf<Task>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChecklistBinding.inflate(inflater, container, false)
        noteId = requireArguments().getLong(NOTE_ID, 0L)
        setupFragmentColors()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadNoteById(noteId)
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)

        if (menuVisible) {
            activity?.hideKeyboard()
        } else if (::binding.isInitialized) {
            (binding.checklistList.adapter as? TasksAdapter)?.finishActMode()
        }
    }

    private fun loadNoteById(noteId: Long) {
        NotesHelper(requireActivity()).getNoteWithId(noteId) { storedNote ->
            if (storedNote != null && activity?.isDestroyed == false) {
                note = storedNote

                try {
                    val taskType = object : TypeToken<List<Task>>() {}.type
                    tasks = Gson().fromJson<ArrayList<Task>>(storedNote.getNoteStoredValue(requireActivity()), taskType) ?: ArrayList(1)

                    tasks = tasks.toMutableList() as ArrayList<Task>
                    val sorting = config?.sorting ?: 0
                    if (sorting and SORT_BY_CUSTOM == 0 && config?.moveDoneChecklistItems == true) {
                        tasks.sortBy { it.isDone }
                    }

                    setupFragment()
                } catch (e: Exception) {
                    migrateCheckListOnFailure(storedNote)
                }
            }
        }
    }

    private fun migrateCheckListOnFailure(note: Note) {
        tasks.clear()

        note.getNoteStoredValue(requireActivity())?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() }?.forEachIndexed { index, value ->
            tasks.add(
                Task(
                    id = index,
                    title = value,
                    isDone = false
                )
            )
        }

        saveTasks(tasks)
    }

    private fun setupFragment() {
        if (activity == null || requireActivity().isFinishing) {
            return
        }

        setupFragmentColors()
        checkLockState()
        setupAdapter()
    }

    private fun setupFragmentColors() {
        val adjustedPrimaryColor = requireActivity().getProperPrimaryColor()
        binding.checklistFab.apply {
            setColors(
                requireActivity().getProperTextColor(),
                adjustedPrimaryColor,
                adjustedPrimaryColor.getContrastColor()
            )

            setOnClickListener {
                showNewItemDialog()
                (binding.checklistList.adapter as? TasksAdapter)?.finishActMode()
            }
        }

        binding.fragmentPlaceholder.setTextColor(requireActivity().getProperTextColor())
        binding.fragmentPlaceholder2.apply {
            setTextColor(adjustedPrimaryColor)
            underlineText()
            setOnClickListener {
                showNewItemDialog()
            }
        }
    }

    override fun checkLockState() {
        if (note == null) {
            return
        }

        binding.apply {
            checklistContentHolder.beVisibleIf(!note!!.isLocked() || shouldShowLockedContent)
            checklistFab.beVisibleIf(!note!!.isLocked() || shouldShowLockedContent)
            setupLockedViews(this.toCommonBinding(), note!!)
        }
    }

    private fun showNewItemDialog() {
        NewChecklistItemDialog(activity as SimpleActivity) { titles ->
            var currentMaxId = tasks.maxByOrNull { item -> item.id }?.id ?: 0
            val newItems = ArrayList<Task>()

            titles.forEach { title ->
                title.split("\n").map { it.trim() }.filter { it.isNotBlank() }.forEach { row ->
                    newItems.add(Task(currentMaxId + 1, System.currentTimeMillis(), row, false))
                    currentMaxId++
                }
            }

            if (config?.addNewChecklistItemsTop == true) {
                tasks.addAll(0, newItems)
            } else {
                tasks.addAll(newItems)
            }

            saveNote()
            setupAdapter()
        }
    }

    private fun setupAdapter() {
        updateUIVisibility()
        Task.sorting = requireContext().config.sorting
        if (Task.sorting and SORT_BY_CUSTOM == 0) {
            tasks.sort()
            if (context?.config?.moveDoneChecklistItems == true) {
                tasks.sortBy { it.isDone }
            }
        }

        var tasksAdapter = binding.checklistList.adapter as? TasksAdapter
        if (tasksAdapter == null) {
            tasksAdapter = TasksAdapter(
                activity = activity as SimpleActivity,
                listener = this,
                recyclerView = binding.checklistList,
                itemClick = ::toggleCompletion
            )
            binding.checklistList.adapter = tasksAdapter
        }

        tasksAdapter.submitList(tasks.toList())
    }

    private fun toggleCompletion(any: Any) {
        val item = any as Task
        val index = tasks.indexOf(item)
        if (index != -1) {
            tasks[index] = item.copy(isDone = !item.isDone)
            saveNote {
                loadNoteById(noteId)
            }
        }
    }

    private fun saveNote(callback: () -> Unit = {}) {
        if (note == null) {
            return
        }

        if (note!!.path.isNotEmpty() && !note!!.path.startsWith("content://") && !File(note!!.path).exists()) {
            return
        }

        if (context == null || activity == null) {
            return
        }

        if (note != null) {
            note!!.value = getTasks()

            ensureBackgroundThread {
                saveNoteValue(note!!, note!!.value)
                context?.updateWidgets()
                activity?.runOnUiThread(callback)
            }
        }
    }

    fun removeDoneItems() {
        tasks = tasks.filter { !it.isDone }.toMutableList() as ArrayList<Task>
        saveNote()
        setupAdapter()
    }

    private fun updateUIVisibility() {
        binding.apply {
            fragmentPlaceholder.beVisibleIf(tasks.isEmpty())
            fragmentPlaceholder2.beVisibleIf(tasks.isEmpty())
            checklistList.beVisibleIf(tasks.isNotEmpty())
        }
    }

    fun getTasks() = Gson().toJson(tasks)

    override fun saveTasks(updatedTasks: List<Task>, callback: () -> Unit) {
        tasks = updatedTasks.toMutableList()
        saveNote(callback = callback)
    }

    override fun refreshItems() {
        loadNoteById(noteId)
        setupAdapter()
    }

    private fun FragmentChecklistBinding.toCommonBinding(): CommonNoteBinding = this.let {
        object : CommonNoteBinding {
            override val root: View = it.root
            override val noteLockedLayout: View = it.noteLockedLayout
            override val noteLockedImage: ImageView = it.noteLockedImage
            override val noteLockedLabel: TextView = it.noteLockedLabel
            override val noteLockedShow: TextView = it.noteLockedShow
        }
    }
}
