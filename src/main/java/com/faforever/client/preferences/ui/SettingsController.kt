package com.faforever.client.preferences.ui

import com.faforever.client.chat.ChatColorMode
import com.faforever.client.chat.ChatFormat
import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.fx.StringListCell
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.Action
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.Severity
import com.faforever.client.notification.TransientNotification
import com.faforever.client.preferences.LocalizationPrefs
import com.faforever.client.preferences.NotificationsPrefs
import com.faforever.client.preferences.Preferences
import com.faforever.client.preferences.Preferences.UnitDataBaseType
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.TimeInfo
import com.faforever.client.preferences.ToastPosition
import com.faforever.client.settings.LanguageItemController
import com.faforever.client.theme.Theme
import com.faforever.client.theme.UiService
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent
import com.faforever.client.user.UserService
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.stage.Screen
import javafx.util.converter.NumberStringConverter
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.text.NumberFormat
import java.util.Collections
import java.util.Locale
import java.util.Objects
import java.util.stream.Collectors

import com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class SettingsController(private val userService: UserService, private val preferencesService: PreferencesService, private val uiService: UiService,
                         private val i18n: I18n, private val eventBus: EventBus, private val notificationService: NotificationService,
                         private val platformService: PlatformService, private val clientProperties: ClientProperties) : Controller<Node> {

    var executableDecoratorField: TextField? = null
    var executionDirectoryField: TextField? = null
    var colorModeToggleGroup: ToggleGroup? = null
    var customColorsToggle: Toggle? = null
    var randomColorsToggle: Toggle? = null
    var defaultColorsToggle: Toggle? = null
    var hideFoeToggle: Toggle? = null
    var gamePortTextField: TextField? = null
    var gameLocationTextField: TextField? = null
    var autoDownloadMapsToggle: Toggle? = null
    var maxMessagesTextField: TextField? = null
    var imagePreviewToggle: Toggle? = null
    var enableNotificationsToggle: Toggle? = null
    var enableSoundsToggle: Toggle? = null
    var displayFriendOnlineToastCheckBox: CheckBox? = null
    var displayFriendOfflineToastCheckBox: CheckBox? = null
    var playFriendOnlineSoundCheckBox: CheckBox? = null
    var playFriendOfflineSoundCheckBox: CheckBox? = null
    var displayFriendJoinsGameToastCheckBox: CheckBox? = null
    var displayFriendPlaysGameToastCheckBox: CheckBox? = null
    var playFriendJoinsGameSoundCheckBox: CheckBox? = null
    var playFriendPlaysGameSoundCheckBox: CheckBox? = null
    var displayPmReceivedToastCheckBox: CheckBox? = null
    var displayLadder1v1ToastCheckBox: CheckBox? = null
    var playPmReceivedSoundCheckBox: CheckBox? = null
    var afterGameReviewCheckBox: CheckBox? = null
    override var root: Region? = null
    var themeComboBox: ComboBox<Theme>? = null
    var rememberLastTabToggle: Toggle? = null
    var toastPositionToggleGroup: ToggleGroup? = null
    var toastScreenComboBox: ComboBox<Screen>? = null
    var bottomLeftToastButton: ToggleButton? = null
    var topRightToastButton: ToggleButton? = null
    var topLeftToastButton: ToggleButton? = null
    var bottomRightToastButton: ToggleButton? = null
    var currentPasswordField: PasswordField? = null
    var newPasswordField: PasswordField? = null
    var confirmPasswordField: PasswordField? = null
    var timeComboBox: ComboBox<TimeInfo>? = null
    var chatComboBox: ComboBox<ChatFormat>? = null
    var passwordChangeErrorLabel: Label? = null
    var passwordChangeSuccessLabel: Label? = null
    var unitDatabaseComboBox: ComboBox<UnitDataBaseType>? = null
    var notifyOnAtMentionOnlyToggle: Toggle? = null
    var languagesContainer: Pane? = null
    private var selectedThemeChangeListener: ChangeListener<Theme>? = null
    private var currentThemeChangeListener: ChangeListener<Theme>? = null
    private val availableLanguagesListener: InvalidationListener

    init {

        availableLanguagesListener = { observable ->
            val localization = preferencesService.preferences!!.localization
            val currentLocale = localization.language
            val nodes = i18n.availableLanguages.stream()
                    .map { locale ->
                        val controller = uiService.loadFxml<LanguageItemController>("theme/settings/language_item.fxml")
                        controller.setLocale(locale)
                        controller.setOnSelectedListener(Consumer<Locale> { this.onLanguageSelected(it) })
                        controller.setSelected(locale == currentLocale)
                        controller.root
                    }
                    .collect<List<Node>, Any>(Collectors.toList())
            languagesContainer!!.children.setAll(nodes)
        }
    }

    /**
     * Disables preferences that should not be enabled since they are not supported yet.
     */
    private fun temporarilyDisableUnsupportedSettings(preferences: Preferences) {
        val notification = preferences.notification
        notification.isFriendOnlineSoundEnabled = false
        notification.isFriendOfflineSoundEnabled = false
        notification.isFriendOfflineSoundEnabled = false
        notification.isFriendPlaysGameSoundEnabled = false
        notification.isFriendPlaysGameToastEnabled = false
    }

    private fun setSelectedToastPosition(newValue: ToastPosition) {
        when (newValue) {
            ToastPosition.TOP_RIGHT -> toastPositionToggleGroup!!.selectToggle(topRightToastButton)
            ToastPosition.BOTTOM_RIGHT -> toastPositionToggleGroup!!.selectToggle(bottomRightToastButton)
            ToastPosition.BOTTOM_LEFT -> toastPositionToggleGroup!!.selectToggle(bottomLeftToastButton)
            ToastPosition.TOP_LEFT -> toastPositionToggleGroup!!.selectToggle(topLeftToastButton)
        }
    }

    override fun initialize() {
        eventBus.register(this)
        themeComboBox!!.setButtonCell(StringListCell(Function<Theme, String> { it.getDisplayName() }))
        themeComboBox!!.setCellFactory { param -> StringListCell(Function<Theme, String> { it.getDisplayName() }) }

        toastScreenComboBox!!.setButtonCell(screenListCell())
        toastScreenComboBox!!.setCellFactory { param -> screenListCell() }
        toastScreenComboBox!!.setItems(Screen.getScreens())
        val integerNumberFormat = NumberFormat.getIntegerInstance()
        integerNumberFormat.isGroupingUsed = false

        val preferences = preferencesService.preferences
        temporarilyDisableUnsupportedSettings(preferences!!)

        JavaFxUtil.bindBidirectional(maxMessagesTextField!!.textProperty(), preferences.chat.maxMessagesProperty(), NumberStringConverter(integerNumberFormat))
        imagePreviewToggle!!.selectedProperty().bindBidirectional(preferences.chat.previewImageUrlsProperty())
        enableNotificationsToggle!!.selectedProperty().bindBidirectional(preferences.notification.transientNotificationsEnabledProperty())

        hideFoeToggle!!.selectedProperty().bindBidirectional(preferences.chat.hideFoeMessagesProperty())

        JavaFxUtil.addListener(preferences.chat.chatColorModeProperty()) { observable, oldValue, newValue -> setSelectedColorMode(newValue) }
        setSelectedColorMode(preferences.chat.chatColorMode)

        colorModeToggleGroup!!.selectedToggleProperty().addListener { observable, oldValue, newValue ->
            if (newValue === defaultColorsToggle) {
                preferences.chat.chatColorMode = ChatColorMode.DEFAULT
            }
            if (newValue === customColorsToggle) {
                preferences.chat.chatColorMode = ChatColorMode.CUSTOM
            }
            if (newValue === randomColorsToggle) {
                preferences.chat.chatColorMode = ChatColorMode.RANDOM
            }
        }

        currentThemeChangeListener = { observable, oldValue, newValue -> themeComboBox!!.selectionModel.select(newValue) }
        selectedThemeChangeListener = { observable, oldValue, newValue -> uiService.setTheme(newValue) }

        JavaFxUtil.addListener(preferences.notification.toastPositionProperty()) { observable, oldValue, newValue -> setSelectedToastPosition(newValue) }
        setSelectedToastPosition(preferences.notification.toastPosition)
        toastPositionToggleGroup!!.selectedToggleProperty().addListener { observable, oldValue, newValue ->
            if (newValue === topLeftToastButton) {
                preferences.notification.toastPosition = ToastPosition.TOP_LEFT
            }
            if (newValue === topRightToastButton) {
                preferences.notification.toastPosition = ToastPosition.TOP_RIGHT
            }
            if (newValue === bottomLeftToastButton) {
                preferences.notification.toastPosition = ToastPosition.BOTTOM_LEFT
            }
            if (newValue === bottomRightToastButton) {
                preferences.notification.toastPosition = ToastPosition.BOTTOM_RIGHT
            }
        }
        configureTimeSetting(preferences)
        configureChatSetting(preferences)
        configureLanguageSelection()
        configureThemeSelection(preferences)
        configureRememberLastTab(preferences)
        configureToastScreen(preferences)

        displayFriendOnlineToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendOnlineToastEnabledProperty())
        displayFriendOfflineToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendOfflineToastEnabledProperty())
        displayFriendJoinsGameToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendJoinsGameToastEnabledProperty())
        displayFriendPlaysGameToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendPlaysGameToastEnabledProperty())
        displayPmReceivedToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.privateMessageToastEnabledProperty())
        displayLadder1v1ToastCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.ladder1v1ToastEnabledProperty())
        playFriendOnlineSoundCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendOnlineSoundEnabledProperty())
        playFriendOfflineSoundCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendOfflineSoundEnabledProperty())
        playFriendJoinsGameSoundCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendJoinsGameSoundEnabledProperty())
        playFriendPlaysGameSoundCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.friendPlaysGameSoundEnabledProperty())
        playPmReceivedSoundCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.privateMessageSoundEnabledProperty())
        afterGameReviewCheckBox!!.selectedProperty().bindBidirectional(preferences.notification.afterGameReviewEnabledProperty())

        notifyOnAtMentionOnlyToggle!!.selectedProperty().bindBidirectional(preferences.notification.notifyOnAtMentionOnlyEnabledProperty())
        enableSoundsToggle!!.selectedProperty().bindBidirectional(preferences.notification.soundsEnabledProperty())
        gamePortTextField!!.textProperty().bindBidirectional(preferences.forgedAlliance.portProperty(), NumberStringConverter(integerNumberFormat))
        gameLocationTextField!!.textProperty().bindBidirectional<Path>(preferences.forgedAlliance.pathProperty(), PATH_STRING_CONVERTER)
        autoDownloadMapsToggle!!.selectedProperty().bindBidirectional(preferences.forgedAlliance.autoDownloadMapsProperty())

        executableDecoratorField!!.textProperty().bindBidirectional(preferences.forgedAlliance.executableDecoratorProperty())
        executionDirectoryField!!.textProperty().bindBidirectional<Path>(preferences.forgedAlliance.executionDirectoryProperty(), PATH_STRING_CONVERTER)

        passwordChangeErrorLabel!!.isVisible = false

        initUnitDatabaseSelection(preferences)
    }

    private fun initUnitDatabaseSelection(preferences: Preferences) {
        unitDatabaseComboBox!!.setButtonCell(StringListCell { unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey()) })
        unitDatabaseComboBox!!.setCellFactory { param -> StringListCell { unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey()) } }
        unitDatabaseComboBox!!.setItems(FXCollections.observableArrayList(*UnitDataBaseType.values()))
        unitDatabaseComboBox!!.isFocusTraversable = true

        val unitDataBaseTypeChangeListener = { observable, oldValue, newValue -> unitDatabaseComboBox!!.selectionModel.select(newValue) }
        unitDataBaseTypeChangeListener.changed(null, null, preferences.unitDataBaseType)
        JavaFxUtil.addListener(preferences.unitDataBaseTypeProperty(), unitDataBaseTypeChangeListener)

        unitDatabaseComboBox!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            preferences.unitDataBaseType = newValue
            preferencesService.storeInBackground()
        }
    }

    private fun configureTimeSetting(preferences: Preferences) {
        timeComboBox!!.buttonCell = StringListCell { timeInfo -> i18n.get(timeInfo.displayNameKey) }
        timeComboBox!!.setCellFactory { param -> StringListCell { timeInfo -> i18n.get(timeInfo.displayNameKey) } }
        timeComboBox!!.items = FXCollections.observableArrayList(*TimeInfo.values())
        timeComboBox!!.isDisable = false
        timeComboBox!!.isFocusTraversable = true
        timeComboBox!!.selectionModel.select(preferences.chat.timeFormat)
    }

    fun onTimeFormatSelected() {
        log.debug("A new time format was selected: {}", timeComboBox!!.value)
        val preferences = preferencesService.preferences
        preferences!!.chat.timeFormat = timeComboBox!!.value
        preferencesService.storeInBackground()
    }


    private fun configureChatSetting(preferences: Preferences) {
        chatComboBox!!.setButtonCell(StringListCell { chatFormat -> i18n.get(chatFormat.getI18nKey()) })
        chatComboBox!!.setCellFactory { param -> StringListCell { chatFormat -> i18n.get(chatFormat.getI18nKey()) } }
        chatComboBox!!.setItems(FXCollections.observableArrayList(*ChatFormat.values()))
        chatComboBox!!.selectionModel.select(preferences.chat.chatFormat)
    }

    fun onChatFormatSelected() {
        log.debug("A new chat format was selected: {}", chatComboBox!!.value)
        val preferences = preferencesService.preferences
        preferences!!.chat.chatFormat = chatComboBox!!.value
        preferencesService.storeInBackground()
    }

    private fun screenListCell(): StringListCell<Screen> {
        return StringListCell { screen -> i18n.get("settings.screenFormat", Screen.getScreens().indexOf(screen) + 1) }
    }

    private fun setSelectedColorMode(newValue: ChatColorMode) {
        when (newValue) {
            ChatColorMode.DEFAULT -> colorModeToggleGroup!!.selectToggle(defaultColorsToggle)
            ChatColorMode.CUSTOM -> colorModeToggleGroup!!.selectToggle(customColorsToggle)
            ChatColorMode.RANDOM -> colorModeToggleGroup!!.selectToggle(randomColorsToggle)
        }
    }

    private fun configureRememberLastTab(preferences: Preferences) {
        JavaFxUtil.bindBidirectional(rememberLastTabToggle!!.selectedProperty(), preferences.rememberLastTabProperty())
    }

    private fun configureThemeSelection(preferences: Preferences) {
        themeComboBox!!.items = FXCollections.observableArrayList(uiService.availableThemes)

        val currentTheme = themeComboBox!!.items.stream()
                .filter { theme -> theme.displayName == preferences.themeName }
                .findFirst().orElse(UiService.DEFAULT_THEME)
        themeComboBox!!.selectionModel.select(currentTheme)

        themeComboBox!!.selectionModel.selectedItemProperty().addListener(selectedThemeChangeListener)
        JavaFxUtil.addListener(uiService.currentThemeProperty(), WeakChangeListener(currentThemeChangeListener!!))
    }

    private fun configureLanguageSelection() {
        i18n.availableLanguages.addListener(WeakInvalidationListener(availableLanguagesListener))
        availableLanguagesListener.invalidated(i18n.availableLanguages)
    }

    @VisibleForTesting
    internal fun onLanguageSelected(locale: Locale) {
        val localizationPrefs = preferencesService.preferences!!.localization
        if (locale == localizationPrefs.language) {
            return
        }
        log.debug("A new language was selected: {}", locale)
        localizationPrefs.setLanguage(locale)
        preferencesService.storeInBackground()

        availableLanguagesListener.invalidated(i18n.availableLanguages)

        notificationService.addNotification(PersistentNotification(
                i18n.get(locale, "settings.languages.restart.message"),
                Severity.WARN,
                listOf(Action(i18n.get(locale, "settings.languages.restart")
                ) { event ->
                    Platform.exit()
                    // FIXME reload application (stage & application context)
                })))
    }

    private fun configureToastScreen(preferences: Preferences) {
        toastScreenComboBox!!.selectionModel.select(preferences.notification.toastScreen)
        preferences.notification.toastScreenProperty().bind(Bindings.createIntegerBinding({ Screen.getScreens().indexOf(toastScreenComboBox!!.value) }, toastScreenComboBox!!.valueProperty()))
    }

    fun onSelectGameLocation() {
        eventBus.post(GameDirectoryChooseEvent())
    }

    fun onSelectExecutionDirectory() {
        // TODO implement
    }

    fun onChangePasswordClicked() {
        passwordChangeSuccessLabel!!.isVisible = false
        passwordChangeErrorLabel!!.isVisible = false

        if (currentPasswordField!!.text.isEmpty()) {
            passwordChangeErrorLabel!!.isVisible = true
            passwordChangeErrorLabel!!.text = i18n.get("settings.account.currentPassword.empty")
            return
        }

        if (newPasswordField!!.text.isEmpty()) {
            passwordChangeErrorLabel!!.isVisible = true
            passwordChangeErrorLabel!!.text = i18n.get("settings.account.newPassword.empty")
            return
        }

        if (newPasswordField!!.text != confirmPasswordField!!.text) {
            passwordChangeErrorLabel!!.isVisible = true
            passwordChangeErrorLabel!!.text = i18n.get("settings.account.confirmPassword.mismatch")
            return
        }

        userService.changePassword(currentPasswordField!!.text, newPasswordField!!.text).future
                .thenAccept { aVoid ->
                    passwordChangeSuccessLabel!!.isVisible = true
                    currentPasswordField!!.text = ""
                    newPasswordField!!.text = ""
                    confirmPasswordField!!.text = ""
                }.exceptionally { throwable ->
                    passwordChangeErrorLabel!!.isVisible = true
                    passwordChangeErrorLabel!!.text = i18n.get("settings.account.changePassword.error", throwable.cause.getLocalizedMessage())
                    null
                }
    }

    fun onPreviewToastButtonClicked() {
        notificationService.addNotification(TransientNotification(
                i18n.get("settings.notifications.toastPreview.title"),
                i18n.get("settings.notifications.toastPreview.text")
        ))
    }

    fun onHelpUsButtonClicked() {
        platformService.showDocument(clientProperties.getTranslationProjectUrl())
    }
}

