/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph.config

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SchemeManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ToolbarDecorator
import io.paleocrafter.chronomorph.Chronomorph
import io.paleocrafter.chronomorph.ChronomorphSettings
import io.paleocrafter.chronomorph.DaylightCycle
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

class ChronomorphConfigurable : Configurable {
    private lateinit var panel: JPanel
    private lateinit var useDayCycleCheckBox: JCheckBox
    private lateinit var latitudeField: JFormattedTextField
    private lateinit var longitudeField: JFormattedTextField
    private lateinit var dayThemeComboBox: JComboBox<UIManager.LookAndFeelInfo>
    private lateinit var nightThemeComboBox: JComboBox<UIManager.LookAndFeelInfo>
    private lateinit var dayColorSchemeComboBox: JComboBox<EditorColorsScheme>
    private lateinit var nightColorSchemeComboBox: JComboBox<EditorColorsScheme>
    private lateinit var sunriseLabel: JLabel
    private lateinit var sunsetLabel: JLabel
    private lateinit var entryTable: ChronoEntryTable
    private lateinit var entryPanel: JPanel

    @Nls
    override fun getDisplayName() = "Chronomorph"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent {
        entryTable = ChronoEntryTable()
        entryPanel.add(
            ToolbarDecorator.createDecorator(entryTable)
                .setAddAction { entryTable.addEntry() }
                .setRemoveAction { entryTable.removeEntry() }
                .setEditAction { entryTable.editEntry() }
                .disableUpDownActions().createPanel(),
            BorderLayout.CENTER)
        return panel
    }

    private fun init() {
        val settings = ChronomorphSettings.instance

        val decimalFormat = DecimalFormat("#.#")
        decimalFormat.maximumFractionDigits = 6
        val latNumberFormatter = NumberFormatter(decimalFormat)
        latNumberFormatter.minimum = -90.0
        latNumberFormatter.maximum = 90.0
        val longNumberFormatter = NumberFormatter(decimalFormat)
        longNumberFormatter.minimum = -180.0
        longNumberFormatter.maximum = 180.0
        latitudeField.formatterFactory = DefaultFormatterFactory(latNumberFormatter)
        longitudeField.formatterFactory = DefaultFormatterFactory(longNumberFormatter)
        latitudeField.value = 0
        longitudeField.value = 0

        dayThemeComboBox.renderer = ChronoEntryEditor.ThemeComboBoxRenderer()
        nightThemeComboBox.renderer = ChronoEntryEditor.ThemeComboBoxRenderer()

        dayThemeComboBox.addItem(null)
        nightThemeComboBox.addItem(null)
        val themes = LafManager.getInstance().installedLookAndFeels
        for (theme in themes) {
            dayThemeComboBox.addItem(theme)
            nightThemeComboBox.addItem(theme)
        }

        dayColorSchemeComboBox.renderer = ChronoEntryEditor.ColorSchemeComboBoxRenderer()
        nightColorSchemeComboBox.renderer = ChronoEntryEditor.ColorSchemeComboBoxRenderer()

        dayColorSchemeComboBox.addItem(null)
        nightColorSchemeComboBox.addItem(null)
        val colorSchemes = EditorColorsManager.getInstance().allSchemes
        for (scheme in colorSchemes) {
            dayColorSchemeComboBox.addItem(scheme)
            nightColorSchemeComboBox.addItem(scheme)
        }

        updateDaylightCycle(settings.latitude, settings.longitude)

        useDayCycleCheckBox.isSelected = settings.useDayCycle

        latitudeField.text = settings.latitude
        longitudeField.text = settings.longitude

        dayThemeComboBox.selectedItem = settings.daySettings.themeInfo
        nightThemeComboBox.selectedItem = settings.nightSettings.themeInfo
        dayColorSchemeComboBox.selectedItem = settings.daySettings.colorSchemeInfo
        nightColorSchemeComboBox.selectedItem = settings.nightSettings.colorSchemeInfo

        entryTable.init(settings.chronoEntries)

        latitudeField.isEnabled = settings.useDayCycle
        longitudeField.isEnabled = settings.useDayCycle

        dayThemeComboBox.isEnabled = settings.useDayCycle
        nightThemeComboBox.isEnabled = settings.useDayCycle
        dayColorSchemeComboBox.isEnabled = settings.useDayCycle
        nightColorSchemeComboBox.isEnabled = settings.useDayCycle

        entryTable.isEnabled = !settings.useDayCycle

        useDayCycleCheckBox.addChangeListener {
            val selected = useDayCycleCheckBox.isSelected
            latitudeField.isEnabled = selected
            longitudeField.isEnabled = selected

            dayThemeComboBox.isEnabled = selected
            nightThemeComboBox.isEnabled = selected
            dayColorSchemeComboBox.isEnabled = selected
            nightColorSchemeComboBox.isEnabled = selected

            entryTable.isEnabled = !selected
        }

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                updateDaylightCycle(latitudeField.value.toString(), longitudeField.value.toString())
            }
        }

        latitudeField.document.addDocumentListener(listener)
        longitudeField.document.addDocumentListener(listener)
    }

    private fun updateDaylightCycle(latitude: String, longitude: String) {
        val cycle = DaylightCycle.getCycle(latitude, longitude) ?: DaylightCycle.DEFAULT
        sunriseLabel.text = cycle.sunrise.format(DateTimeFormatter.ISO_LOCAL_TIME)
        sunsetLabel.text = cycle.sunset.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    override fun isModified(): Boolean {
        val settings = ChronomorphSettings.instance

        return useDayCycleCheckBox.isSelected != settings.useDayCycle
            || latitudeField.text != settings.latitude
            || longitudeField.text != settings.longitude
            || dayThemeComboBox.value?.className != settings.daySettings.theme
            || dayColorSchemeComboBox.value?.name != settings.daySettings.colorScheme
            || nightThemeComboBox.value?.className != settings.nightSettings.theme
            || nightColorSchemeComboBox.value?.name != settings.nightSettings.colorScheme
            || entryTable.entries != settings.chronoEntries
    }

    override fun apply() {
        val settings = ChronomorphSettings.instance

        settings.useDayCycle = useDayCycleCheckBox.isSelected

        settings.latitude = latitudeField.text
        settings.longitude = longitudeField.text

        settings.daySettings = settings.daySettings.copy(
            theme = dayThemeComboBox.value?.className,
            colorScheme = dayColorSchemeComboBox.value?.name
        )
        settings.nightSettings = settings.nightSettings.copy(
            theme = nightThemeComboBox.value?.className,
            colorScheme = nightColorSchemeComboBox.value?.name
        )

        settings.chronoEntries.clear()
        settings.chronoEntries.addAll(entryTable.entries)
        Chronomorph.instance.check()
    }

    override fun reset() {
        init()
    }
}

val UIManager.LookAndFeelInfo?.userDescription: String
    get() = this?.name ?: "Leave unchanged"

val EditorColorsScheme?.userDescription: String
    get() = this?.let { StringUtil.trimStart(it.name, SchemeManager.EDITABLE_COPY_PREFIX) } ?: "Use theme default"

val <T> JComboBox<T>.value: T?
    get() = this.selectedItem as T?
