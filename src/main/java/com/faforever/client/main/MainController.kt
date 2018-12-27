package com.faforever.client.main

import ch.micheljung.fxborderlessscene.borderless.BorderlessScene
import com.faforever.client.chat.event.UnreadPrivateMessageEvent
import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.game.GamePathHandler
import com.faforever.client.game.GameService
import com.faforever.client.i18n.I18n
import com.faforever.client.login.LoginController
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.NavigationItem
import com.faforever.client.main.event.Open1v1Event
import com.faforever.client.news.UnreadNewsEvent
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.ImmediateNotificationController
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.PersistentNotificationsController
import com.faforever.client.notification.Severity
import com.faforever.client.notification.TransientNotification
import com.faforever.client.notification.TransientNotificationsController
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.WindowPrefs
import com.faforever.client.preferences.ui.SettingsController
import com.faforever.client.rankedmatch.MatchmakerMessage
import com.faforever.client.remote.domain.RatingRange
import com.faforever.client.theme.UiService
import com.faforever.client.ui.StageHolder
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent
import com.faforever.client.update.ClientUpdateService
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.google.common.annotations.VisibleForTesting
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.jfoenix.animation.alert.JFXAlertAnimation
import com.jfoenix.controls.JFXAlert
import javafx.animation.FadeTransition
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.geometry.Bounds
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.control.Labeled
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.Popup
import javafx.stage.PopupWindow.AnchorLocation
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.Collections
import java.util.Objects
import java.util.Optional
import java.util.function.Function

import com.github.nocatch.NoCatch.noCatch
import javafx.application.Platform.runLater

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO divide and conquer
class MainController @Inject
constructor(private val preferencesService: PreferencesService, private val i18n: I18n, private val notificationService: NotificationService,
            private val playerService: PlayerService, private val gameService: GameService, private val clientUpdateService: ClientUpdateService,
            private val uiService: UiService, private val eventBus: EventBus, clientProperties: ClientProperties, private val gamePathHandler: GamePathHandler,
            private val platformService: PlatformService) : Controller<Node> {
    private val viewCache: Cache<NavigationItem, AbstractViewController<*>>
    private val mainWindowTitle: String
    private val ratingBeta: Int
    var mainHeaderPane: Pane? = null
    var notificationsBadge: Labeled? = null
    var contentPane: Pane? = null
    var newsButton: ToggleButton? = null
    var chatButton: ToggleButton? = null
    var playButton: ToggleButton? = null
    var vaultButton: ToggleButton? = null
    var leaderboardsButton: ToggleButton? = null
    var tournamentsButton: ToggleButton? = null
    var unitsButton: ToggleButton? = null
    override var root: Pane? = null
    var contentWrapperPane: StackPane? = null
    var mainNavigation: ToggleGroup? = null
    @VisibleForTesting
    var transientNotificationsPopup: Popup
    @VisibleForTesting
    internal var persistentNotificationsPopup: Popup
    private var currentItem: NavigationItem? = null
    private var mainScene: BorderlessScene? = null

    private val transientNotificationAreaBounds: Rectangle2D
        get() {
            val screens = Screen.getScreens()

            val toastScreenIndex = preferencesService.preferences!!.notification.toastScreen
            val screen: Screen
            if (toastScreenIndex < screens.size) {
                screen = screens[Math.max(0, toastScreenIndex)]
            } else {
                screen = Screen.getPrimary()
            }
            return screen.visualBounds
        }

    init {

        this.mainWindowTitle = clientProperties.getMainWindowTitle()
        this.ratingBeta = clientProperties.getTrueSkill().getBeta()
        this.viewCache = CacheBuilder.newBuilder().build()
    }

    override fun initialize() {
        newsButton!!.userData = NavigationItem.NEWS
        chatButton!!.userData = NavigationItem.CHAT
        playButton!!.userData = NavigationItem.PLAY
        vaultButton!!.userData = NavigationItem.VAULT
        leaderboardsButton!!.userData = NavigationItem.LEADERBOARD
        tournamentsButton!!.userData = NavigationItem.TOURNAMENTS
        unitsButton!!.userData = NavigationItem.UNITS
        eventBus.register(this)

        val persistentNotificationsController = uiService.loadFxml<PersistentNotificationsController>("theme/persistent_notifications.fxml")
        persistentNotificationsPopup = Popup()
        persistentNotificationsPopup.content.setAll(persistentNotificationsController.root)
        persistentNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_TOP_RIGHT
        persistentNotificationsPopup.isAutoFix = true
        persistentNotificationsPopup.isAutoHide = true

        val transientNotificationsController = uiService.loadFxml<TransientNotificationsController>("theme/transient_notifications.fxml")
        transientNotificationsPopup = Popup()
        transientNotificationsPopup.isAutoFix = true
        transientNotificationsPopup.scene.root.styleClass.add("transient-notification")
        transientNotificationsPopup.content.setAll(transientNotificationsController.root)

        transientNotificationsController.root!!.children.addListener(ToastDisplayer(transientNotificationsController))

        updateNotificationsButton(emptyList())
        notificationService.addPersistentNotificationListener { change -> runLater { updateNotificationsButton(change.set) } }
        notificationService.addImmediateNotificationListener { notification -> runLater { displayImmediateNotification(notification) } }
        notificationService.addTransientNotificationListener { notification -> runLater { transientNotificationsController.addNotification(notification) } }
        gameService.addOnRankedMatchNotificationListener(Consumer<MatchmakerMessage> { this.onMatchmakerMessage(it) })
        // Always load chat immediately so messages or joined channels don't need to be cached until we display them.
        getView(NavigationItem.CHAT)
    }

    @Subscribe
    fun onLoginSuccessEvent(event: LoginSuccessEvent) {
        runLater { this.enterLoggedInState() }
    }

    @Subscribe
    fun onLoggedOutEvent(event: LoggedOutEvent) {
        runLater { this.enterLoggedOutState() }
    }

    @Subscribe
    fun onUnreadNews(event: UnreadNewsEvent) {
        runLater { newsButton!!.pseudoClassStateChanged(HIGHLIGHTED, event.hasUnreadNews()) }
    }

    @Subscribe
    fun onUnreadMessage(event: UnreadPrivateMessageEvent) {
        runLater { chatButton!!.pseudoClassStateChanged(HIGHLIGHTED, currentItem != NavigationItem.CHAT) }
    }

    private fun displayView(controller: AbstractViewController<*>, navigateEvent: NavigateEvent) {
        val node = controller.root
        val children = contentPane!!.children

        if (!children.contains(node)) {
            children.add(node)
            JavaFxUtil.setAnchors(node, 0.0)
        }

        Optional.ofNullable(currentItem).ifPresent { item -> getView(item).hide() }
        controller.display(navigateEvent)
    }

    /**
     * Updates the number displayed in the notifications button and sets its CSS pseudo class based on the highest
     * notification `Severity` of all current notifications.
     */
    private fun updateNotificationsButton(notifications: Collection<PersistentNotification>) {
        JavaFxUtil.assertApplicationThread()

        val size = notifications.size
        notificationsBadge!!.isVisible = size != 0
        notificationsBadge!!.text = i18n.number(size)

        val highestSeverity = notifications.stream()
                .map { it.severity }
                .max { obj, e -> obj.compareTo(e) }
                .orElse(null)

        notificationsBadge!!.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_CLASS, highestSeverity == Severity.INFO)
        notificationsBadge!!.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_CLASS, highestSeverity == Severity.WARN)
        notificationsBadge!!.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_CLASS, highestSeverity == Severity.ERROR)

        val ft = FadeTransition(Duration.millis(666.0), notificationsBadge)
        ft.fromValue = 0.0
        ft.toValue = 1.0
        ft.cycleCount = 1
        ft.isAutoReverse = true
        ft.play()
    }

    private fun onMatchmakerMessage(message: MatchmakerMessage) {
        if (message.queues == null
                || gameService.gameRunningProperty().get()
                || !preferencesService.preferences!!.notification.ladder1v1ToastEnabled
                || !playerService.currentPlayer.isPresent) {
            return
        }

        val currentPlayer = playerService.currentPlayer.get()

        val deviationFor80PercentQuality = (ratingBeta / 2.5f).toInt()
        val deviationFor75PercentQuality = (ratingBeta / 1.25f).toInt()
        val leaderboardRatingDeviation = currentPlayer.leaderboardRatingDeviation

        val ratingRangesSupplier: Function<MatchmakerMessage.MatchmakerQueue, List<RatingRange>>
        if (leaderboardRatingDeviation <= deviationFor80PercentQuality) {
            ratingRangesSupplier = Function<MatchmakerQueue, List<RatingRange>> { it.getBoundary80s() }
        } else if (leaderboardRatingDeviation <= deviationFor75PercentQuality) {
            ratingRangesSupplier = Function<MatchmakerQueue, List<RatingRange>> { it.getBoundary75s() }
        } else {
            return
        }

        val leaderboardRatingMean = currentPlayer.leaderboardRatingMean
        var showNotification = false
        for (matchmakerQueue in message.queues!!) {
            if ("ladder1v1" != matchmakerQueue.queueName) {
                continue
            }
            val ratingRanges = ratingRangesSupplier.apply(matchmakerQueue)

            for (ratingRange in ratingRanges) {
                if (ratingRange.min <= leaderboardRatingMean && leaderboardRatingMean <= ratingRange.max) {
                    showNotification = true
                    break
                }
            }
        }

        if (!showNotification) {
            return
        }

        notificationService.addNotification(TransientNotification(
                i18n.get("ranked1v1.notification.title"),
                i18n.get("ranked1v1.notification.message"),
                uiService.getThemeImage(UiService.LADDER_1V1_IMAGE)
        ) { event -> eventBus.post(Open1v1Event()) })
    }

    fun display() {
        eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0))

        val stage = StageHolder.stage

        mainScene = uiService.createScene(stage, root)
        stage.scene = mainScene

        val mainWindowPrefs = preferencesService.preferences!!.mainWindow
        val x = mainWindowPrefs.x
        val y = mainWindowPrefs.y
        val width = mainWindowPrefs.width
        val height = mainWindowPrefs.height

        stage.minWidth = 10.0
        stage.minHeight = 10.0
        stage.width = width.toDouble()
        stage.height = height.toDouble()
        stage.show()

        hideSplashScreen()
        enterLoggedOutState()

        val screensForRectangle = Screen.getScreensForRectangle(x, y, width.toDouble(), height.toDouble())
        if (screensForRectangle.isEmpty()) {
            JavaFxUtil.centerOnScreen(stage)
        } else {
            stage.x = x
            stage.y = y
        }
        if (mainWindowPrefs.maximized) {
            getMainScene().maximizeStage()
        }
        registerWindowListeners()
    }

    private fun enterLoggedOutState() {
        val stage = StageHolder.stage
        stage.title = i18n.get("login.title")
        val loginController = uiService.loadFxml<LoginController>("theme/login.fxml")

        getMainScene().setContent(loginController.root)
        getMainScene().setMoveControl(loginController.root)
        loginController.display()
    }

    private fun getMainScene(): BorderlessScene {
        if (mainScene == null) {
            throw IllegalStateException("'borderlessScene' isn't initialized yet, make sure to call display() first")
        }
        return mainScene
    }

    private fun registerWindowListeners() {
        val stage = StageHolder.stage
        val mainWindowPrefs = preferencesService.preferences!!.mainWindow
        JavaFxUtil.addListener(stage.maximizedProperty()) { observable, oldValue, newValue ->
            if (!newValue) {
                stage.width = mainWindowPrefs.width.toDouble()
                stage.height = mainWindowPrefs.height.toDouble()
                val screensForRectangle = Screen.getScreensForRectangle(mainWindowPrefs.x, mainWindowPrefs.y, mainWindowPrefs.width.toDouble(), mainWindowPrefs.height.toDouble())
                if (screensForRectangle.isEmpty()) {
                    JavaFxUtil.centerOnScreen(stage)
                } else {
                    stage.x = mainWindowPrefs.x
                    stage.y = mainWindowPrefs.y
                }

            }
            mainWindowPrefs.maximized = newValue!!
            preferencesService.storeInBackground()
        }
        JavaFxUtil.addListener(stage.heightProperty()) { observable, oldValue, newValue ->
            if (!stage.isMaximized) {
                mainWindowPrefs.height = newValue.toInt()
                preferencesService.storeInBackground()
            }
        }
        JavaFxUtil.addListener(stage.widthProperty()) { observable, oldValue, newValue ->
            if (!stage.isMaximized) {
                mainWindowPrefs.width = newValue.toInt()
                preferencesService.storeInBackground()
            }
        }
        JavaFxUtil.addListener(stage.xProperty()) { observable ->
            if (!stage.isMaximized) {
                mainWindowPrefs.x = stage.x
                preferencesService.storeInBackground()
            }
        }
        JavaFxUtil.addListener(stage.yProperty()) { observable ->
            if (!stage.isMaximized) {
                mainWindowPrefs.y = stage.y
                preferencesService.storeInBackground()
            }
        }
    }

    private fun enterLoggedInState() {
        val stage = StageHolder.stage
        stage.title = mainWindowTitle
        getMainScene().setContent(root)
        getMainScene().setMoveControl(root)

        clientUpdateService.checkForUpdateInBackground()

        gamePathHandler.detectAndUpdateGamePath()
        restoreLastView()
    }

    private fun restoreLastView() {
        val navigationItem: NavigationItem
        if (preferencesService.preferences!!.rememberLastTab) {
            val mainWindowPrefs = preferencesService.preferences!!.mainWindow
            navigationItem = Optional.ofNullable(NavigationItem.fromString(mainWindowPrefs.lastView)).orElse(NavigationItem.NEWS)
        } else {
            navigationItem = NavigationItem.NEWS
        }
        eventBus.post(NavigateEvent(navigationItem))
    }

    fun onNotificationsButtonClicked() {
        val screenBounds = notificationsBadge!!.localToScreen(notificationsBadge!!.boundsInLocal)
        persistentNotificationsPopup.show(notificationsBadge!!.scene.window, screenBounds.maxX, screenBounds.maxY)
    }

    fun onSettingsSelected() {
        val stage = Stage(StageStyle.UNDECORATED)
        stage.initOwner(root!!.scene.window)

        val settingsController = uiService.loadFxml<SettingsController>("theme/settings/settings.fxml")

        val borderlessScene = uiService.createScene(stage, settingsController.root)
        stage.scene = borderlessScene
        stage.showingProperty().addListener(object : ChangeListener {
            fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean?, newValue: Boolean?) {
                if ((!newValue)!!) {
                    stage.showingProperty().removeListener(this)
                    preferencesService.storeInBackground()
                }
            }
        })

        stage.title = i18n.get("settings.windowTitle")
        stage.show()
    }

    fun onExitItemSelected() {
        Platform.exit()
    }

    fun onNavigateButtonClicked(event: ActionEvent) {
        eventBus.post(NavigateEvent((event.source as Node).userData as NavigationItem))
    }

    @Subscribe
    fun onNavigateEvent(navigateEvent: NavigateEvent) {
        val item = navigateEvent.getItem()

        val controller = getView(item)
        displayView(controller, navigateEvent)

        mainNavigation!!.toggles.stream()
                .filter { toggle -> toggle.userData === navigateEvent.getItem() }
                .findFirst()
                .ifPresent { toggle -> toggle.isSelected = true }

        currentItem = item
        preferencesService.preferences!!.mainWindow.lastView = item.name
        preferencesService.storeInBackground()
    }

    private fun getView(item: NavigationItem): AbstractViewController<*> {
        return noCatch<AbstractViewController<*>> { viewCache.get(item) { uiService.loadFxml(item.fxmlFile) } }
    }

    fun onRevealMapFolder() {
        val mapPath = preferencesService.preferences!!.forgedAlliance.customMapsDirectory
        this.platformService.reveal(mapPath)
    }

    fun onRevealModFolder() {
        val modPath = preferencesService.preferences!!.forgedAlliance.modsDirectory
        this.platformService.reveal(modPath)
    }

    fun onRevealLogFolder() {
        val logPath = preferencesService.fafLogDirectory
        this.platformService.reveal(logPath)
    }

    fun onChat(actionEvent: ActionEvent) {
        chatButton!!.pseudoClassStateChanged(HIGHLIGHTED, false)
        onNavigateButtonClicked(actionEvent)
    }

    private fun displayImmediateNotification(notification: ImmediateNotification) {
        val dialog = JFXAlert<Any>(getMainScene().window as Stage)

        val controller = (uiService.loadFxml<Controller<*>>("theme/immediate_notification.fxml") as ImmediateNotificationController)
                .setNotification(notification)
                .setCloseListener { dialog.close() }

        dialog.setContent(controller.jfxDialogLayout)
        dialog.animation = JFXAlertAnimation.TOP_ANIMATION
        dialog.show()
    }

    inner class ToastDisplayer(private val transientNotificationsController: TransientNotificationsController) : InvalidationListener {

        override fun invalidated(observable: Observable) {
            val enabled = preferencesService.preferences!!.notification.isTransientNotificationsEnabled
            if (transientNotificationsController.root!!.children.isEmpty() || !enabled) {
                transientNotificationsPopup.hide()
                return
            }

            val visualBounds = transientNotificationAreaBounds
            var anchorX = visualBounds.maxX - 1
            var anchorY = visualBounds.maxY - 1
            when (preferencesService.preferences!!.notification.toastPositionProperty().get()) {
                ToastPosition.BOTTOM_RIGHT -> transientNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_BOTTOM_RIGHT
                ToastPosition.TOP_RIGHT -> {
                    transientNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_TOP_RIGHT
                    anchorY = visualBounds.minY
                }
                ToastPosition.BOTTOM_LEFT -> {
                    transientNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_BOTTOM_LEFT
                    anchorX = visualBounds.minX
                }
                ToastPosition.TOP_LEFT -> {
                    transientNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_TOP_LEFT
                    anchorX = visualBounds.minX
                    anchorY = visualBounds.minY
                }
                else -> transientNotificationsPopup.anchorLocation = AnchorLocation.CONTENT_BOTTOM_RIGHT
            }
            transientNotificationsPopup.show(root!!.scene.window, anchorX, anchorY)
        }
    }

    companion object {
        private val NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info")
        private val NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn")
        private val NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error")
        private val HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted")

        /**
         * Hides the install4j splash screen. The hide method is invoked via reflection to accommodate starting the client
         * without install4j (e.g. on linux).
         */
        private fun hideSplashScreen() {
            try {
                val splashScreenClass = Class.forName("com.install4j.api.launcher.SplashScreen")
                val hideMethod = splashScreenClass.getDeclaredMethod("hide")
                hideMethod.invoke(null)
            } catch (e: ClassNotFoundException) {
                log.debug("No install4j splash screen found to close.")
            } catch (e: NoSuchMethodException) {
                log.error("Couldn't close install4j splash screen.", e)
            } catch (e: IllegalAccessException) {
                log.error("Couldn't close install4j splash screen.", e)
            } catch (e: InvocationTargetException) {
                log.error("Couldn't close install4j splash screen.", e.cause)
            }

        }
    }
}
