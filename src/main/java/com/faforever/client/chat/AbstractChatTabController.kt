package com.faforever.client.chat

import com.faforever.client.audio.AudioService
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.NavigationItem
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.ui.StageHolder
import com.faforever.client.uploader.ImageUploadService
import com.faforever.client.user.UserService
import com.faforever.client.util.IdenticonUtil
import com.faforever.client.util.TimeService
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner
import com.google.common.eventbus.EventBus
import com.google.common.io.CharStreams
import javafx.application.Platform
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.concurrent.Worker
import javafx.css.PseudoClass
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextInputControl
import javafx.scene.control.skin.TabPaneSkin
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import netscape.javascript.JSObject
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

import javax.inject.Inject
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.ArrayList
import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern

import com.faforever.client.theme.UiService.CHAT_CONTAINER
import com.faforever.client.theme.UiService.CHAT_SECTION_COMPACT
import com.faforever.client.theme.UiService.CHAT_SECTION_EXTENDED
import com.faforever.client.theme.UiService.CHAT_TEXT_COMPACT
import com.faforever.client.theme.UiService.CHAT_TEXT_EXTENDED
import com.github.nocatch.NoCatch.noCatch
import com.google.common.html.HtmlEscapers.htmlEscaper
import java.time.temporal.ChronoUnit.MINUTES
import java.util.regex.Pattern.CASE_INSENSITIVE
import javafx.scene.AccessibleAttribute.ITEM_AT_INDEX

/**
 * A chat tab displays messages in a [WebView]. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some of the logic has to be
 * performed in interaction with JavaScript, like when the user clicks a link.
 */
abstract class AbstractChatTabController @Inject
constructor(protected val webViewConfigurer: WebViewConfigurer,
            protected val userService: UserService, protected val chatService: ChatService,
            protected val preferencesService: PreferencesService,
            protected val playerService: PlayerService, protected val audioService: AudioService,
            protected val timeService: TimeService, protected val i18n: I18n,
            private val imageUploadService: ImageUploadService,
            protected val notificationService: NotificationService, protected val reportingService: ReportingService, protected val uiService: UiService,
            private val autoCompletionHelper: AutoCompletionHelper, protected val eventBus: EventBus, private val countryFlagService: CountryFlagService) : Controller<Tab> {

    /**
     * Messages that arrived before the web view was ready. Those are appended as soon as it is ready.
     */
    private val waitingMessages: MutableList<ChatMessage>
    private val unreadMessagesCount: IntegerProperty
    private val resetUnreadMessagesListener: ChangeListener<Boolean>
    private val zoomChangeListener: ChangeListener<Number>
    private val tabPaneFocusedListener: ChangeListener<Boolean>
    private val stageFocusedListener: ChangeListener<Boolean>
    private var lastEntryId: Int = 0
    private var isChatReady: Boolean = false
    /**
     * Either a channel like "#aeolus" or a user like "Visionik".
     */
    var receiver: String? = null
    private var mentionPattern: Pattern? = null
    private var lastMessage: ChatMessage? = null
    private var engine: WebEngine? = null

    abstract override val root: Tab?

    abstract val messagesWebView: WebView

    protected val jsObject: JSObject
        get() = engine!!.executeScript("window") as JSObject

    init {

        waitingMessages = ArrayList()
        unreadMessagesCount = SimpleIntegerProperty()
        resetUnreadMessagesListener = { observable, oldValue, newValue ->
            if (hasFocus()) {
                setUnread(false)
            }
        }
        zoomChangeListener = { observable, oldValue, newValue ->
            preferencesService.preferences!!.chat.setZoom(newValue.toDouble())
            preferencesService.storeInBackground()
        }
        stageFocusedListener = { window, windowFocusOld, windowFocusNew ->
            if (root != null
                    && root!!.tabPane != null
                    && root!!.tabPane.isVisible) {
                messageTextField().requestFocus()
            }
        }
        tabPaneFocusedListener = { focusedTabPane, oldTabPaneFocus, newTabPaneFocus ->
            if (newTabPaneFocus!!) {
                messageTextField().requestFocus()
            }
        }
    }// TODO cut dependencies

    /**
     * Returns true if this chat tab is currently focused by the user. Returns false if a different tab is selected, the
     * user is not in "chat" or if the window has no focus.
     */
    fun hasFocus(): Boolean {
        if (!root!!.isSelected) {
            return false
        }

        val tabPane = root!!.tabPane
        return (tabPane != null
                && JavaFxUtil.isVisibleRecursively(tabPane)
                && tabPane.scene.window.isFocused
                && tabPane.scene.window.isShowing)
    }

    protected fun setUnread(unread: Boolean) {
        val tabPane = root!!.tabPane ?: return
        val skin = tabPane.skin as TabPaneSkin ?: return
        val tabIndex = tabPane.tabs.indexOf(root)
        if (tabIndex == -1) {
            // Tab has been closed
            return
        }
        val tab = skin.queryAccessibleAttribute(ITEM_AT_INDEX, tabIndex) as Node
        tab.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread)

        if (!unread) {
            synchronized(unreadMessagesCount) {
                unreadMessagesCount.value = 0
            }
        }
    }

    protected fun incrementUnreadMessagesCount(delta: Int) {
        synchronized(unreadMessagesCount) {
            unreadMessagesCount.set(unreadMessagesCount.get() + delta)
        }
    }

    override fun initialize() {
        mentionPattern = Pattern.compile("\\b(" + Pattern.quote(userService.username) + ")\\b", CASE_INSENSITIVE)

        initChatView()

        addFocusListeners()
        addImagePasteListener()

        unreadMessagesCount.addListener { observable, oldValue, newValue -> chatService.incrementUnreadMessagesCount(newValue.toInt() - oldValue.toInt()) }
        JavaFxUtil.addListener(StageHolder.stage.focusedProperty(), WeakChangeListener(resetUnreadMessagesListener))
        JavaFxUtil.addListener(root!!.selectedProperty(), WeakChangeListener(resetUnreadMessagesListener))

        autoCompletionHelper.bindTo(messageTextField())

        root!!.onClosed = EventHandler<Event> { this.onClosed(it) }
    }

    protected open fun onClosed(event: Event) {
        // Subclasses may override but need to call super
    }

    /**
     * Registers listeners necessary to focus the message input field when changing to another message tab, changing from
     * another tab to the "chat" tab or re-focusing the window.
     */
    private fun addFocusListeners() {
        JavaFxUtil.addListener(root!!.selectedProperty()) { observable, oldValue, newValue ->
            if (newValue!!) {
                // Since a tab is marked as "selected" before it's rendered, the text field can't be selected yet.
                // So let's schedule the focus to be executed afterwards
                Platform.runLater { messageTextField().requestFocus() }
            }
        }

        JavaFxUtil.addListener(root!!.tabPaneProperty()) { tabPane, oldTabPane, newTabPane ->
            if (newTabPane == null) {
                return@JavaFxUtil.addListener
            }
            JavaFxUtil.addListener(StageHolder.stage.focusedProperty(), WeakChangeListener(stageFocusedListener))
            JavaFxUtil.addListener(newTabPane!!.focusedProperty(), WeakChangeListener(tabPaneFocusedListener))
        }
    }

    private fun addImagePasteListener() {
        val messageTextField = messageTextField()
        messageTextField.setOnKeyReleased { event ->
            if (isPaste(event) && Clipboard.getSystemClipboard().hasImage()) {
                pasteImage()
            }
        }
    }

    abstract fun messageTextField(): TextInputControl

    private fun isPaste(event: KeyEvent): Boolean {
        return event.code == KeyCode.V && event.isShortcutDown || event.code == KeyCode.INSERT && event.isShiftDown
    }

    private fun pasteImage() {
        val messageTextField = messageTextField()
        val currentCaretPosition = messageTextField.caretPosition

        messageTextField.isDisable = true

        val clipboard = Clipboard.getSystemClipboard()
        val image = clipboard.image

        imageUploadService.uploadImageInBackground(image).thenAccept { url ->
            messageTextField.insertText(currentCaretPosition, url)
            messageTextField.isDisable = false
            messageTextField.requestFocus()
            messageTextField.positionCaret(messageTextField.length)
        }.exceptionally { throwable ->
            messageTextField.isDisable = false
            null
        }
    }

    private fun initChatView() {
        val messagesWebView = messagesWebView
        webViewConfigurer.configureWebView(messagesWebView)

        messagesWebView.zoomProperty().addListener(WeakChangeListener(zoomChangeListener))

        configureBrowser(messagesWebView)
        loadChatContainer()
    }

    private fun loadChatContainer() {
        try {
            InputStreamReader(uiService.getThemeFileUrl(CHAT_CONTAINER).openStream()).use { reader ->
                val chatContainerHtml = CharStreams.toString(reader)
                        .replace("{chat-container-js}", CHAT_JS_RESOURCE.url.toExternalForm())
                        .replace("{auto-linker-js}", AUTOLINKER_JS_RESOURCE.url.toExternalForm())
                        .replace("{jquery-js}", JQUERY_JS_RESOURCE.url.toExternalForm())
                        .replace("{jquery-highlight-js}", JQUERY_HIGHLIGHT_JS_RESOURCE.url.toExternalForm())

                engine!!.loadContent(chatContainerHtml)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    private fun configureBrowser(messagesWebView: WebView) {
        engine = messagesWebView.engine

        configureZoomLevel()
        configureLoadListener()
    }

    private fun configureZoomLevel() {
        val zoom = preferencesService.preferences!!.chat.zoom
        if (zoom != null) {
            messagesWebView.zoom = zoom
        }
    }

    private fun configureLoadListener() {
        JavaFxUtil.addListener<State>(engine!!.loadWorker.stateProperty()) { observable, oldValue, newValue ->
            if (newValue != Worker.State.SUCCEEDED) {
                return@JavaFxUtil.addListener
            }
            synchronized(waitingMessages) {
                waitingMessages.forEach(Consumer<ChatMessage> { this@AbstractChatTabController.addMessage(it) })
                waitingMessages.clear()
                isChatReady = true
                onWebViewLoaded()
            }
        }
    }

    protected fun onWebViewLoaded() {
        // Default implementation does nothing, can be overridden by subclass.
    }

    fun onSendMessage() {
        val messageTextField = messageTextField()

        val text = messageTextField.text
        if (StringUtils.isEmpty(text)) {
            return
        }

        if (text.startsWith(ACTION_PREFIX)) {
            sendAction(messageTextField, text)
        } else if (text.startsWith(JOIN_PREFIX)) {
            chatService.joinChannel(text.replaceFirst(Pattern.quote(JOIN_PREFIX).toRegex(), ""))
            messageTextField.clear()
        } else if (text.startsWith(WHOIS_PREFIX)) {
            chatService.whois(text.replaceFirst(Pattern.quote(JOIN_PREFIX).toRegex(), ""))
            messageTextField.clear()
        } else {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val messageTextField = messageTextField()
        messageTextField.isDisable = true

        val text = messageTextField.text
        chatService.sendMessageInBackground(receiver, text).thenAccept { message ->
            messageTextField.clear()
            messageTextField.isDisable = false
            messageTextField.requestFocus()
        }.exceptionally { throwable ->
            logger.warn("Message could not be sent: {}", text, throwable)
            notificationService.addNotification(ImmediateErrorNotification(
                    i18n.get("errorTitle"), i18n.get("chat.sendFailed"), throwable, i18n, reportingService)
            )

            messageTextField.isDisable = false
            messageTextField.requestFocus()
            null
        }
    }

    private fun sendAction(messageTextField: TextInputControl, text: String) {
        messageTextField.isDisable = true

        chatService.sendActionInBackground(receiver, text.replaceFirst(Pattern.quote(ACTION_PREFIX).toRegex(), ""))
                .thenAccept { message ->
                    messageTextField.clear()
                    messageTextField.isDisable = false
                    messageTextField.requestFocus()
                }
                .exceptionally { throwable ->
                    // TODO onDisplay error to user somehow
                    logger.warn("Message could not be sent: {}", text, throwable)
                    messageTextField.isDisable = false
                    null
                }
    }

    open fun onChatMessage(chatMessage: ChatMessage) {
        synchronized(waitingMessages) {
            if (!isChatReady) {
                waitingMessages.add(chatMessage)
            } else {
                Platform.runLater {
                    addMessage(chatMessage)
                    removeTopmostMessages()
                    scrollToBottomIfDesired()
                }
            }
        }
    }

    private fun scrollToBottomIfDesired() {
        engine!!.executeScript("scrollToBottomIfDesired()")
    }

    private fun removeTopmostMessages() {
        val maxMessageItems = preferencesService.preferences!!.chat.maxMessages

        var numberOfMessages = engine!!.executeScript("document.getElementsByClassName('$MESSAGE_ITEM_CLASS').length") as Int
        while (numberOfMessages > maxMessageItems) {
            engine!!.executeScript("document.getElementsByClassName('$MESSAGE_ITEM_CLASS')[0].remove()")
            numberOfMessages--
        }
    }

    /**
     * Either inserts a new chat entry or, if the same user as before sent another message, appends it do the previous
     * entry.
     */
    private fun addMessage(chatMessage: ChatMessage) {
        noCatch {
            if (requiresNewChatSection(chatMessage)) {
                appendChatMessageSection(chatMessage)
            } else {
                appendMessage(chatMessage)
            }
            lastMessage = chatMessage
        }
    }

    private fun requiresNewChatSection(chatMessage: ChatMessage): Boolean {
        return (lastMessage == null
                || !lastMessage!!.getUsername().equals(chatMessage.getUsername())
                || lastMessage!!.getTime().isBefore(chatMessage.getTime().minus(1, MINUTES))
                || lastMessage!!.isAction())
    }

    @Throws(IOException::class)
    private fun appendMessage(chatMessage: ChatMessage) {
        val themeFileUrl: URL
        if (preferencesService.preferences!!.chat.chatFormat == ChatFormat.COMPACT) {
            themeFileUrl = uiService.getThemeFileUrl(CHAT_TEXT_COMPACT)
        } else {
            themeFileUrl = uiService.getThemeFileUrl(CHAT_TEXT_EXTENDED)
        }

        val html = renderHtml(chatMessage, themeFileUrl, null)

        insertIntoContainer(html, "chat-section-$lastEntryId")
    }

    @Throws(IOException::class)
    private fun appendChatMessageSection(chatMessage: ChatMessage) {
        val themeFileURL: URL
        if (preferencesService.preferences!!.chat.chatFormat == ChatFormat.COMPACT) {
            themeFileURL = uiService.getThemeFileUrl(CHAT_SECTION_COMPACT)
        } else {
            themeFileURL = uiService.getThemeFileUrl(CHAT_SECTION_EXTENDED)
        }

        val html = renderHtml(chatMessage, themeFileURL, ++lastEntryId)
        insertIntoContainer(html, MESSAGE_CONTAINER_ID)
        appendMessage(chatMessage)
    }

    @Throws(IOException::class)
    private fun renderHtml(chatMessage: ChatMessage, themeFileUrl: URL, sectionId: Int?): String {
        var html: String
        InputStreamReader(themeFileUrl.openStream()).use { reader -> html = CharStreams.toString(reader) }

        val login = chatMessage.getUsername()
        var avatarUrl = ""
        var clanTag = ""
        var decoratedClanTag = ""
        var countryFlagUrl = ""

        val playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername())
        if (playerOptional.isPresent) {
            val player = playerOptional.get()
            avatarUrl = player.avatarUrl
            countryFlagUrl = countryFlagService.getCountryFlagUrl(player.country)
                    .map<String>(Function<URL, String> { it.toString() })
                    .orElse("")

            if (StringUtils.isNotEmpty(player.clan)) {
                clanTag = player.clan
                decoratedClanTag = i18n.get("chat.clanTagFormat", clanTag)
            }
        }

        val timeString = timeService.asShortTime(chatMessage.getTime())
        html = html.replace("{time}", timeString)
                .replace("{avatar}", StringUtils.defaultString(avatarUrl))
                .replace("{username}", login)
                .replace("{clan-tag}", clanTag)
                .replace("{decorated-clan-tag}", decoratedClanTag)
                .replace("{country-flag}", StringUtils.defaultString(countryFlagUrl))
                .replace("{section-id}", sectionId.toString())

        val cssClasses = ArrayList<String>()
        cssClasses.add(String.format("user-%s", chatMessage.getUsername()))
        if (chatMessage.isAction()) {
            cssClasses.add(ACTION_CSS_CLASS)
        } else {
            cssClasses.add(MESSAGE_CSS_CLASS)
        }

        html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses))

        Optional.ofNullable(getMessageCssClass(login)).ifPresent(Consumer<String> { cssClasses.add(it) })

        var text = htmlEscaper().escape(chatMessage.getMessage()).replace("\\", "\\\\")
        text = convertUrlsToHyperlinks(text)

        val matcher = mentionPattern!!.matcher(text)
        if (matcher.find()) {
            text = matcher.replaceAll("<span class='self'>" + matcher.group(1) + "</span>")
            onMention(chatMessage)
        }

        return html
                .replace("{css-classes}", Joiner.on(' ').join(cssClasses))
                .replace("{inline-style}", getInlineStyle(login))
                // Always replace text last in case the message contains one of the placeholders.
                .replace("{text}", text)
    }

    protected open fun onMention(chatMessage: ChatMessage) {
        // Default implementation does nothing
    }

    protected fun showNotificationIfNecessary(chatMessage: ChatMessage) {
        val stage = StageHolder.stage
        if (stage.isFocused && stage.isShowing) {
            return
        }

        val playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername())
        val identiconSource = playerOptional.map { player -> player.id.toString() }.orElseGet(Supplier<String> { chatMessage.getUsername() })

        notificationService.addNotification(TransientNotification(
                chatMessage.getUsername(),
                chatMessage.getMessage(),
                IdenticonUtil.createIdenticon(identiconSource)
        ) { event ->
            eventBus.post(NavigateEvent(NavigationItem.CHAT))
            stage.toFront()
            root!!.tabPane.selectionModel.select(root)
        }
        )
    }

    open fun getMessageCssClass(login: String): String {
        val playerOptional = playerService.getPlayerForUsername(login)
        return if (!playerOptional.isPresent) {
            CSS_CLASS_CHAT_ONLY
        } else playerOptional.get().socialStatus.cssClass

    }

    protected open fun getInlineStyle(username: String): String {
        // To be overridden by subclasses
        return ""
    }

    @VisibleForTesting
    internal fun createInlineStyleFromColor(messageColor: Color): String {
        return String.format("color: %s;", JavaFxUtil.toRgbCode(messageColor))
    }

    protected fun convertUrlsToHyperlinks(text: String): String {
        return engine!!.executeScript("link('" + text.replace("'", "\\'") + "')") as String
    }

    private fun insertIntoContainer(html: String, containerId: String) {
        (engine!!.executeScript("document.getElementById('$containerId')") as JSObject)
                .call("insertAdjacentHTML", "beforeend", html)
        messagesWebView.requestLayout()
    }

    /**
     * Subclasses may override in order to perform actions when the view is being displayed.
     */
    fun onDisplay() {
        messageTextField().requestFocus()
    }

    /**
     * Subclasses may override in order to perform actions when the view is no longer being displayed.
     */
    fun onHide() {

    }

    companion object {

        internal val CSS_CLASS_CHAT_ONLY = "chat_only"
        private val MESSAGE_CONTAINER_ID = "chat-container"
        private val MESSAGE_ITEM_CLASS = "chat-section"
        private val UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread")
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val CHAT_JS_RESOURCE = ClassPathResource("/js/chat_container.js")
        private val AUTOLINKER_JS_RESOURCE = ClassPathResource("/js/Autolinker.min.js")
        private val JQUERY_JS_RESOURCE = ClassPathResource("js/jquery-2.1.4.min.js")
        private val JQUERY_HIGHLIGHT_JS_RESOURCE = ClassPathResource("js/jquery.highlight-5.closure.js")

        private val ACTION_PREFIX = "/me "
        private val JOIN_PREFIX = "/join "
        private val WHOIS_PREFIX = "/whois "
        /**
         * Added if a message is what IRC calls an "action".
         */
        private val ACTION_CSS_CLASS = "action"
        private val MESSAGE_CSS_CLASS = "message"
    }
}
