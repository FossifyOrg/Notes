package org.fossify.notes.activities

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.openRequestExactAlarmSettings
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.PROTECTION_FINGERPRINT
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.helpers.isSPlus
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.RadioItem
import org.fossify.notes.BuildConfig
import org.fossify.notes.R
import org.fossify.notes.databinding.ActivitySettingsBinding
import org.fossify.notes.dialogs.ExportNotesDialog
import org.fossify.notes.dialogs.ManageAutoBackupsDialog
import org.fossify.notes.extensions.cancelScheduledAutomaticBackup
import org.fossify.notes.extensions.config
import org.fossify.notes.extensions.requestUnlockNotes
import org.fossify.notes.extensions.scheduleNextAutomaticBackup
import org.fossify.notes.extensions.updateWidgets
import org.fossify.notes.extensions.widgetsDB
import org.fossify.notes.helpers.ALL_WIDGET_IDS
import org.fossify.notes.helpers.FONT_SIZE_100_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_125_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_150_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_175_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_200_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_250_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_300_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_50_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_60_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_75_PERCENT
import org.fossify.notes.helpers.FONT_SIZE_90_PERCENT
import org.fossify.notes.helpers.GRAVITY_CENTER
import org.fossify.notes.helpers.GRAVITY_END
import org.fossify.notes.helpers.GRAVITY_START
import org.fossify.notes.helpers.NotesHelper
import org.fossify.notes.models.Note
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val notesFileType = "application/json"
    private val notesImportFileTypes = buildList {
        add("application/json")
        if (!isQPlus()) {
            // Workaround for https://github.com/FossifyOrg/Notes/issues/34
            add("application/octet-stream")
        }
    }

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(
            mainCoordinatorLayout = binding.settingsCoordinator,
            nestedView = binding.settingsHolder,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupAutosaveNotes()
        setupDisplaySuccess()
        setupClickableLinks()
        setupMonospacedFont()
        setupShowKeyboard()
        setupShowNotePicker()
        setupMoveUndoneChecklistItems()
        setupShowWordCount()
        setupEnableLineWrap()
        setupFontSize()
        setupGravity()
        setupCursorPlacement()
        setupIncognitoMode()
        setupCustomizeWidgetColors()
        setupNotesExport()
        setupNotesImport()
        setupEnableAutomaticBackups()
        setupManageAutomaticBackups()
        setupAppPasswordProtection()
        setupNoteDeletionPasswordProtection()
        updateTextColors(binding.settingsNestedScrollview)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsTextLabel,
            binding.settingsStartupLabel,
            binding.settingsSavingLabel,
            binding.settingsSecurityLabel,
            binding.settingsMigratingLabel,
            binding.settingsBackupsLabel,
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                toast(org.fossify.commons.R.string.importing)
                importNotes(uri)
            }
        }

    private val saveDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument(notesFileType)) { uri ->
            if (uri != null) {
                toast(org.fossify.commons.R.string.exporting)
                NotesHelper(this).getNotes { notes ->
                    requestUnlockNotes(notes) { unlockedNotes ->
                        val notLockedNotes = notes.filterNot { it.isLocked() }
                        val notesToExport = unlockedNotes + notLockedNotes
                        exportNotes(notesToExport, uri)
                    }
                }
            }
        }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        if (isTiramisuPlus()) {
            binding.settingsLanguageHolder.beVisible()
            binding.settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        } else {
            binding.settingsLanguageHolder.beGone()
        }
    }

    private fun setupAutosaveNotes() {
        binding.settingsAutosaveNotes.isChecked = config.autosaveNotes
        binding.settingsAutosaveNotesHolder.setOnClickListener {
            binding.settingsAutosaveNotes.toggle()
            config.autosaveNotes = binding.settingsAutosaveNotes.isChecked
        }
    }

    private fun setupDisplaySuccess() {
        binding.settingsDisplaySuccess.isChecked = config.displaySuccess
        binding.settingsDisplaySuccessHolder.setOnClickListener {
            binding.settingsDisplaySuccess.toggle()
            config.displaySuccess = binding.settingsDisplaySuccess.isChecked
        }
    }

    private fun setupClickableLinks() {
        binding.settingsClickableLinks.isChecked = config.clickableLinks
        binding.settingsClickableLinksHolder.setOnClickListener {
            binding.settingsClickableLinks.toggle()
            config.clickableLinks = binding.settingsClickableLinks.isChecked
        }
    }

    private fun setupMonospacedFont() {
        binding.settingsMonospacedFont.isChecked = config.monospacedFont
        binding.settingsMonospacedFontHolder.setOnClickListener {
            binding.settingsMonospacedFont.toggle()
            config.monospacedFont = binding.settingsMonospacedFont.isChecked
            updateWidgets()
        }
    }

    private fun setupShowKeyboard() {
        binding.settingsShowKeyboard.isChecked = config.showKeyboard
        binding.settingsShowKeyboardHolder.setOnClickListener {
            binding.settingsShowKeyboard.toggle()
            config.showKeyboard = binding.settingsShowKeyboard.isChecked
        }
    }

    private fun setupShowNotePicker() {
        NotesHelper(this).getNotes {
            binding.settingsShowNotePickerHolder.beVisibleIf(it.size > 1)
        }

        binding.settingsShowNotePicker.isChecked = config.showNotePicker
        binding.settingsShowNotePickerHolder.setOnClickListener {
            binding.settingsShowNotePicker.toggle()
            config.showNotePicker = binding.settingsShowNotePicker.isChecked
        }
    }

    private fun setupMoveUndoneChecklistItems() {
        binding.settingsMoveUndoneChecklistItems.isChecked = config.moveDoneChecklistItems
        binding.settingsMoveUndoneChecklistItemsHolder.setOnClickListener {
            binding.settingsMoveUndoneChecklistItems.toggle()
            config.moveDoneChecklistItems = binding.settingsMoveUndoneChecklistItems.isChecked
        }
    }

    private fun setupShowWordCount() {
        binding.settingsShowWordCount.isChecked = config.showWordCount
        binding.settingsShowWordCountHolder.setOnClickListener {
            binding.settingsShowWordCount.toggle()
            config.showWordCount = binding.settingsShowWordCount.isChecked
        }
    }

    private fun setupEnableLineWrap() {
        binding.settingsEnableLineWrap.isChecked = config.enableLineWrap
        binding.settingsEnableLineWrapHolder.setOnClickListener {
            binding.settingsEnableLineWrap.toggle()
            config.enableLineWrap = binding.settingsEnableLineWrap.isChecked
        }
    }

    private fun setupFontSize() {
        binding.settingsFontSize.text = getFontSizePercentText(config.fontSizePercentage)
        binding.settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_50_PERCENT, getFontSizePercentText(FONT_SIZE_50_PERCENT)),
                RadioItem(FONT_SIZE_60_PERCENT, getFontSizePercentText(FONT_SIZE_60_PERCENT)),
                RadioItem(FONT_SIZE_75_PERCENT, getFontSizePercentText(FONT_SIZE_75_PERCENT)),
                RadioItem(FONT_SIZE_90_PERCENT, getFontSizePercentText(FONT_SIZE_90_PERCENT)),
                RadioItem(FONT_SIZE_100_PERCENT, getFontSizePercentText(FONT_SIZE_100_PERCENT)),
                RadioItem(FONT_SIZE_125_PERCENT, getFontSizePercentText(FONT_SIZE_125_PERCENT)),
                RadioItem(FONT_SIZE_150_PERCENT, getFontSizePercentText(FONT_SIZE_150_PERCENT)),
                RadioItem(FONT_SIZE_175_PERCENT, getFontSizePercentText(FONT_SIZE_175_PERCENT)),
                RadioItem(FONT_SIZE_200_PERCENT, getFontSizePercentText(FONT_SIZE_200_PERCENT)),
                RadioItem(FONT_SIZE_250_PERCENT, getFontSizePercentText(FONT_SIZE_250_PERCENT)),
                RadioItem(FONT_SIZE_300_PERCENT, getFontSizePercentText(FONT_SIZE_300_PERCENT))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSizePercentage) {
                config.fontSizePercentage = it as Int
                binding.settingsFontSize.text = getFontSizePercentText(config.fontSizePercentage)
                updateWidgets()
            }
        }
    }

    private fun getFontSizePercentText(fontSizePercentage: Int): String = "$fontSizePercentage%"

    private fun setupGravity() {
        binding.settingsGravity.text = getGravityText()
        binding.settingsGravityHolder.setOnClickListener {
            val items = listOf(GRAVITY_START, GRAVITY_CENTER, GRAVITY_END).map {
                RadioItem(it, getGravityOptionLabel(it))
            }
            RadioGroupDialog(this@SettingsActivity, ArrayList(items), config.gravity) {
                config.gravity = it as Int
                binding.settingsGravity.text = getGravityText()
                updateWidgets()
            }
        }
    }

    private fun getGravityOptionLabel(gravity: Int): String {
        val leftToRightDirection = TextUtilsCompat
            .getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
        val leftRightLabels = listOf(R.string.left, R.string.right)
        val startEndLabels = if (leftToRightDirection) {
            leftRightLabels
        } else {
            leftRightLabels.reversed()
        }

        return getString(
            when (gravity) {
                GRAVITY_START -> startEndLabels.first()
                GRAVITY_CENTER -> R.string.center
                else -> startEndLabels.last()
            }
        )
    }

    private fun getGravityText() = getGravityOptionLabel(config.gravity)

    private fun setupCursorPlacement() {
        binding.settingsCursorPlacement.isChecked = config.placeCursorToEnd
        binding.settingsCursorPlacementHolder.setOnClickListener {
            binding.settingsCursorPlacement.toggle()
            config.placeCursorToEnd = binding.settingsCursorPlacement.isChecked
        }
    }

    private fun setupCustomizeWidgetColors() {
        var allWidgetIds = intArrayOf()

        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                putExtra(ALL_WIDGET_IDS, allWidgetIds)
                startActivity(this)
            }
        }

        ensureBackgroundThread {
            val widgets = widgetsDB.getWidgets().filter { it.widgetId != 0 }
            allWidgetIds = widgets.map { it.widgetId }.toIntArray()
        }
    }

    private fun setupIncognitoMode() {
        binding.settingsUseIncognitoMode.isChecked = config.useIncognitoMode
        binding.settingsUseIncognitoModeHolder.setOnClickListener {
            binding.settingsUseIncognitoMode.toggle()
            config.useIncognitoMode = binding.settingsUseIncognitoMode.isChecked
        }
    }

    private fun setupNotesExport() {
        binding.settingsExportNotesHolder.setOnClickListener {
            ExportNotesDialog(this) { filename ->
                saveDocument.launch("$filename.json")
            }
        }
    }

    private fun setupNotesImport() {
        binding.settingsImportNotesHolder.setOnClickListener {
            getContent.launch(notesImportFileTypes.toTypedArray())
        }
    }

    private fun exportNotes(notes: List<Note>, uri: Uri) {
        if (notes.isEmpty()) {
            toast(org.fossify.commons.R.string.no_entries_for_exporting)
        } else {
            try {
                val outputStream = contentResolver.openOutputStream(uri)!!

                val jsonString = Json.encodeToString(notes)
                outputStream.use {
                    it.write(jsonString.toByteArray())
                }
                toast(org.fossify.commons.R.string.exporting_successful)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun importNotes(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)!!.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
            val objects = Json.decodeFromString<List<Note>>(jsonString)
            if (objects.isEmpty()) {
                toast(org.fossify.commons.R.string.no_entries_for_importing)
                return
            }
            NotesHelper(this).importNotes(this, objects) { importResult ->
                when (importResult) {
                    NotesHelper.ImportResult.IMPORT_OK -> toast(org.fossify.commons.R.string.importing_successful)
                    NotesHelper.ImportResult.IMPORT_PARTIAL -> toast(org.fossify.commons.R.string.importing_some_entries_failed)
                    NotesHelper.ImportResult.IMPORT_NOTHING_NEW -> toast(org.fossify.commons.R.string.no_new_items)
                    else -> toast(org.fossify.commons.R.string.importing_failed)
                }
            }
        } catch (_: SerializationException) {
            toast(org.fossify.commons.R.string.invalid_file_format)
        } catch (_: IllegalArgumentException) {
            toast(org.fossify.commons.R.string.invalid_file_format)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun setupEnableAutomaticBackups() {
        binding.settingsBackupsLabel.beVisibleIf(isRPlus())
        binding.settingsEnableAutomaticBackupsHolder.beVisibleIf(isRPlus())
        binding.settingsEnableAutomaticBackups.isChecked = config.autoBackup
        binding.settingsEnableAutomaticBackupsHolder.setOnClickListener {
            val wasBackupDisabled = !config.autoBackup
            if (wasBackupDisabled) {
                maybeRequestExactAlarmPermission {
                    ManageAutoBackupsDialog(
                        activity = this,
                        onSuccess = {
                            enableOrDisableAutomaticBackups(true)
                            scheduleNextAutomaticBackup()
                        }
                    )
                }
            } else {
                cancelScheduledAutomaticBackup()
                enableOrDisableAutomaticBackups(false)
            }
        }
    }

    private fun setupManageAutomaticBackups() {
        binding.settingsManageAutomaticBackupsHolder.beVisibleIf(isRPlus() && config.autoBackup)
        binding.settingsManageAutomaticBackupsHolder.setOnClickListener {
            ManageAutoBackupsDialog(
                activity = this,
                onSuccess = {
                    scheduleNextAutomaticBackup()
                }
            )
        }
    }

    private fun enableOrDisableAutomaticBackups(enable: Boolean) {
        config.autoBackup = enable
        binding.settingsEnableAutomaticBackups.isChecked = enable
        binding.settingsManageAutomaticBackupsHolder.beVisibleIf(enable)
    }

    private fun maybeRequestExactAlarmPermission(callback: () -> Unit = {}) {
        if (isSPlus()) {
            val alarmManager: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                callback()
            } else {
                PermissionRequiredDialog(
                    activity = this,
                    textId = R.string.allow_alarm_automatic_backups,
                    positiveActionCallback = {
                        openRequestExactAlarmSettings(BuildConfig.APPLICATION_ID)
                    },
                )
            }
        } else {
            callback()
        }
    }

    private fun setupAppPasswordProtection() {
        binding.settingsAppPasswordProtection.isChecked = config.isAppPasswordProtectionOn
        binding.settingsAppPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isAppPasswordProtectionOn) {
                config.appProtectionType
            } else {
                SHOW_ALL_TABS
            }

            SecurityDialog(
                activity = this,
                requiredHash = config.appPasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    binding.settingsAppPasswordProtection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.appProtectionType == PROTECTION_FINGERPRINT) {
                                org.fossify.commons.R.string.fingerprint_setup_successfully
                            } else {
                                org.fossify.commons.R.string.protection_setup_successfully
                            }

                        ConfirmationDialog(
                            activity = this,
                            message = "",
                            messageId = confirmationTextId,
                            positive = org.fossify.commons.R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }

    private fun setupNoteDeletionPasswordProtection() {
        binding.settingsNoteDeletionPasswordProtection.isChecked =
            config.isDeletePasswordProtectionOn

        binding.settingsNoteDeletionPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isDeletePasswordProtectionOn) {
                config.deleteProtectionType
            } else {
                SHOW_ALL_TABS
            }

            SecurityDialog(
                activity = this,
                requiredHash = config.deletePasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isDeletePasswordProtectionOn
                    binding.settingsNoteDeletionPasswordProtection.isChecked =
                        !hasPasswordProtection
                    config.isDeletePasswordProtectionOn = !hasPasswordProtection
                    config.deletePasswordHash = if (hasPasswordProtection) "" else hash
                    config.deleteProtectionType = type

                    if (config.isDeletePasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.deleteProtectionType == PROTECTION_FINGERPRINT) {
                                org.fossify.commons.R.string.fingerprint_setup_successfully
                            } else {
                                org.fossify.commons.R.string.protection_setup_successfully
                            }

                        ConfirmationDialog(
                            activity = this,
                            message = "",
                            messageId = confirmationTextId,
                            positive = org.fossify.commons.R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }
}
