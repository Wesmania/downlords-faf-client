package com.faforever.client.chat

import com.faforever.client.audio.AudioService
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerOnlineEvent
import com.faforever.client.player.PlayerService
import com.faforever.client.player.SocialStatus
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.uploader.ImageUploadService
import com.faforever.client.user.UserService
import com.faforever.client.util.TimeService
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.gson.Gson
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.SetChangeListener
import javafx.collections.WeakSetChangeListener
import javafx.collections.transformation.FilteredList
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.geometry.Bounds
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.Tab
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.control.ToggleButton
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import javafx.stage.Popup
import javafx.stage.PopupWindow
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.HashMap
import java.util.Optional
import java.util.TreeMap
import java.util.function.Predicate
import java.util.stream.Collectors

import com.faforever.client.chat.ChatColorMode.DEFAULT
import com.faforever.client.player.SocialStatus.FOE
import java.util.Locale.US

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ChannelTabController// TODO cut dependencies
(userService: UserService, chatService: ChatService,
 preferencesService: PreferencesService,
 playerService: PlayerService, audioService: AudioService, timeService: TimeService,
 i18n: I18n, imageUploadService: ImageUploadService,
 notificationService: NotificationService, reportingService: ReportingService,
 uiService: UiService, autoCompletionHelper: AutoCompletionHelper, eventBus: EventBus,
 webViewConfigurer: WebViewConfigurer,
 countryFlagService: CountryFlagService) : AbstractChatTabController(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService, timeService, i18n, imageUploadService, notificationService, reportingService, uiService, autoCompletionHelper, eventBus, countryFlagService) {

    /** Prevents garbage collection of listener. Key is the username.  */
    private val chatColorModeChangeListener: ChangeListener<ChatColorMode>

    /** Prevents garbage collection of listeners. Key is the username.  */
    private val hideFoeMessagesListeners: MutableMap<String, Collection<ChangeListener<Boolean>>>

    /** Prevents garbage collection of listeners. Key is the username.  */
    private val socialStatusMessagesListeners: MutableMap<String, Collection<ChangeListener<SocialStatus>>>

    /** Prevents garbage collection of listeners. Key is the username.  */
    private val colorPropertyListeners: MutableMap<String, Collection<ChangeListener<Color>>>

    /** Maps a chat user category to a list of all user items that belong to it.  */
    private val categoriesToUserListItems: MutableMap<ChatUserCategory, List<CategoryOrChatUserListItem>>

    /** Maps a chat user category to the list items that represent the respective category within the chat user list.  */
    private val categoriesToCategoryListItems: MutableMap<ChatUserCategory, CategoryOrChatUserListItem>

    /** Maps usernames to all chat user list items that belong to that user.  */
    private val userNamesToListItems: MutableMap<String, List<CategoryOrChatUserListItem>>

    private val filteredChatUserList: FilteredList<CategoryOrChatUserListItem>

    /** The list of chat user (or category) items that backs the chat user list view.  */
    private val chatUserListItems: ObservableList<CategoryOrChatUserListItem>

    var advancedUserFilter: ToggleButton? = null
    var searchFieldContainer: HBox? = null
    var closeSearchFieldButton: Button? = null
    var searchField: TextField? = null
    var channelTabScrollPaneVBox: VBox? = null
    override var root: Tab? = null
    override var messagesWebView: WebView? = null
    var userSearchTextField: TextField? = null
    var messageTextField: TextField? = null
    var chatUserListView: ListView<CategoryOrChatUserListItem>? = null

    private var channel: Channel? = null
    private var filterUserPopup: Popup? = null
    private var userFilterController: UserFilterController? = null
    private var usersChangeListener: MapChangeListener<String, ChatChannelUser>? = null
    /** For a set of usernames.  */
    private var moderatorsChangedListener: SetChangeListener<String>? = null

    init {

        hideFoeMessagesListeners = HashMap()
        socialStatusMessagesListeners = HashMap()
        colorPropertyListeners = HashMap()
        categoriesToUserListItems = HashMap()
        categoriesToCategoryListItems = HashMap()
        userNamesToListItems = TreeMap(String.CASE_INSENSITIVE_ORDER)
        chatUserListItems = FXCollections.observableArrayList()
        filteredChatUserList = FilteredList(chatUserListItems)

        chatColorModeChangeListener = { observable, oldValue, newValue ->
            if (newValue != DEFAULT) {
                setAllMessageColors()
            } else {
                removeAllMessageColors()
            }
        }

        val categoryObjects = createCategoryTreeObjects()
        categoryObjects.forEach { categoryItem ->
            categoriesToCategoryListItems[categoryItem.getCategory()] = categoryItem
            categoriesToUserListItems[categoryItem.getCategory()] = ArrayList()
        }
        chatUserListItems.addAll(categoryObjects)
    }

    fun setChannel(channel: Channel) {
        Assert.state(this.channel == null, "Channel has already been set")
        this.channel = channel

        val channelName = channel.name
        receiver = channelName
        root!!.id = channelName
        root!!.text = channelName

        moderatorsChangedListener = { change ->
            if (change.wasAdded()) {
                userNamesToListItems[change.getElementAdded()].forEach(Consumer<CategoryOrChatUserListItem> { this.addModerator(it) })
            } else if (change.wasRemoved()) {
                userNamesToListItems[change.getElementAdded()].forEach(Consumer<CategoryOrChatUserListItem> { this.removeModerator(it) })
            }
        }
        JavaFxUtil.addListener(channel.moderators, WeakSetChangeListener(moderatorsChangedListener!!))

        usersChangeListener = { change ->
            if (change.wasAdded()) {
                onUserJoinedChannel(change.getValueAdded())
            } else if (change.wasRemoved()) {
                onUserLeft(change.getValueRemoved().getUsername())
            }
            updateUserCount(change.getMap().size)
        }
        updateUserCount(channel.users.size)

        chatService.addUsersListener(channelName, usersChangeListener)

        // Maybe there already were some users; fetch them
        channel.users.forEach(Consumer<ChatChannelUser> { this@ChannelTabController.onUserJoinedChannel(it) })

        root!!.setOnCloseRequest { event ->
            chatService.leaveChannel(channel.name)
            chatService.removeUsersListener(channelName, usersChangeListener)
        }

        searchFieldContainer!!.visibleProperty().bind(searchField!!.visibleProperty())
        closeSearchFieldButton!!.visibleProperty().bind(searchField!!.visibleProperty())
        addSearchFieldListener()
    }

    private fun removeModerator(item: CategoryOrChatUserListItem) {
        updateCssClass(item.getUser())
    }

    private fun updateUserCount(count: Int) {
        Platform.runLater { userSearchTextField!!.promptText = i18n.get("chat.userCount", count) }
    }

    private fun addModerator(item: CategoryOrChatUserListItem) {
        updateCssClass(item.getUser())
    }

    override fun initialize() {
        super.initialize()

        userSearchTextField!!.textProperty().addListener { observable, oldValue, newValue -> filterChatUsers(newValue) }

        channelTabScrollPaneVBox!!.minWidth = preferencesService.preferences!!.chat.channelTabScrollPaneWidth.toDouble()
        channelTabScrollPaneVBox!!.prefWidth = preferencesService.preferences!!.chat.channelTabScrollPaneWidth.toDouble()
        JavaFxUtil.addListener(preferencesService.preferences!!.chat.chatColorModeProperty(), chatColorModeChangeListener)
        addUserFilterPopup()

        chatUserListView!!.setItems(filteredChatUserList)
        chatUserListView!!.setCellFactory { param -> ChatUserListCell(uiService) }
    }

    override fun onClosed(event: Event) {
        super.onClosed(event)
        JavaFxUtil.removeListener(preferencesService.preferences!!.chat.chatColorModeProperty(), chatColorModeChangeListener)
    }

    private fun createCategoryTreeObjects(): List<CategoryOrChatUserListItem> {
        return Arrays.stream(ChatUserCategory.values())
                .map { chatUserCategory -> CategoryOrChatUserListItem(chatUserCategory, null) }
                .collect<List<CategoryOrChatUserListItem>, Any>(Collectors.toList())
    }

    private fun setAllMessageColors() {
        val userToColor = HashMap<String, String>()
        channel!!.users.stream().filter { chatUser -> chatUser.color != null }.forEach { chatUser -> userToColor[chatUser.username] = JavaFxUtil.toRgbCode(chatUser.color) }
        jsObject.call("setAllMessageColors", Gson().toJson(userToColor))
    }

    private fun removeAllMessageColors() {
        jsObject.call("removeAllMessageColors")
    }

    @VisibleForTesting
    internal fun isUsernameMatch(user: ChatChannelUser): Boolean {
        val lowerCaseSearchString = user.username.toLowerCase(US)
        return lowerCaseSearchString.contains(userSearchTextField!!.text.toLowerCase(US))
    }

    public override fun messageTextField(): TextInputControl? {
        return messageTextField
    }

    public override fun onMention(chatMessage: ChatMessage) {
        if (preferencesService.preferences!!.notification.notifyOnAtMentionOnlyEnabled && !chatMessage.getMessage().contains("@" + userService.username)) {
            return
        }
        if (!hasFocus()) {
            audioService.playChatMentionSound()
            showNotificationIfNecessary(chatMessage)
            incrementUnreadMessagesCount(1)
            setUnread(true)
        }
    }

    public override fun getMessageCssClass(login: String): String {
        val chatUser = chatService.getChatUser(login, channel!!.name)
        val currentPlayerOptional = playerService.currentPlayer

        if (currentPlayerOptional.isPresent) {
            return ""
        }

        return if (chatUser.isModerator) {
            CSS_CLASS_MODERATOR
        } else super.getMessageCssClass(login)

    }

    private fun addUserFilterPopup() {
        filterUserPopup = Popup()
        filterUserPopup!!.isAutoFix = false
        filterUserPopup!!.isAutoHide = true
        filterUserPopup!!.anchorLocation = PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT

        userFilterController = uiService.loadFxml("theme/chat/user_filter.fxml")
        userFilterController!!.setChannelController(this)
        userFilterController!!.filterAppliedProperty().addListener { observable, oldValue, newValue -> advancedUserFilter!!.isSelected = newValue!! }
        filterUserPopup!!.content.setAll(userFilterController!!.root)
    }

    private fun updateUserMessageColor(chatUser: ChatChannelUser) {
        var color = ""
        if (chatUser.color != null) {
            color = JavaFxUtil.toRgbCode(chatUser.color)
        }
        jsObject.call("updateUserMessageColor", chatUser.username, color)
    }

    private fun removeUserMessageClass(chatUser: ChatChannelUser, cssClass: String) {
        //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
        if (cssClass.isEmpty()) {
            return
        }
        Platform.runLater { jsObject.call("removeUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, chatUser.username), cssClass) }

    }

    private fun addUserMessageClass(player: ChatChannelUser, cssClass: String) {
        Platform.runLater { jsObject.call("addUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.username), cssClass) }
    }

    private fun updateUserMessageDisplay(chatUser: ChatChannelUser, display: String) {
        Platform.runLater { jsObject.call("updateUserMessageDisplay", chatUser.username, display) }
    }

    /** Filters by username "contains" case insensitive.  */
    private fun filterChatUsers(searchString: String) {
        setUserFilter({ listItem ->
            if (Strings.isNullOrEmpty(searchString)) {
                return@setUserFilter true
            }

            val user = listItem.getUser()

            listItem.getCategory() != null || user.getUsername().toLowerCase(US).contains(searchString.toLowerCase(US))
        })
    }

    private fun associateChatUserWithPlayer(player: Player, chatUser: ChatChannelUser) {
        chatUser.setPlayer(player)
        player.chatChannelUsers.add(chatUser)

        updateCssClass(chatUser)
        updateInChatUserList(chatUser)

        val chatPrefs = preferencesService.preferences!!.chat
        JavaFxUtil.addListener(player.socialStatusProperty(), createWeakSocialStatusListener(chatPrefs, chatUser, player))
    }

    private fun onUserJoinedChannel(chatUser: ChatChannelUser) {
        val playerOptional = playerService.getPlayerForUsername(chatUser.username)
        if (playerOptional.isPresent) {
            associateChatUserWithPlayer(playerOptional.get(), chatUser)
        } else {
            updateInChatUserList(chatUser)
        }

        val weakHideFoeMessagesListener = createWeakHideFoeMessagesListener(chatUser)
        val weakColorPropertyListener = createWeakColorPropertyListener(chatUser)

        val chatPrefs = preferencesService.preferences!!.chat
        JavaFxUtil.addListener(chatUser.colorProperty(), weakColorPropertyListener)
        JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), weakHideFoeMessagesListener)

        Platform.runLater {
            weakColorPropertyListener.changed(chatUser.colorProperty(), null, chatUser.color)
            weakHideFoeMessagesListener.changed(chatPrefs.hideFoeMessagesProperty(), null, chatPrefs.hideFoeMessages)
        }
    }

    private fun updateInChatUserList(chatUser: ChatChannelUser) {
        Platform.runLater {
            (userNamesToListItems as java.util.Map<String, List<CategoryOrChatUserListItem>>).computeIfAbsent(chatUser.username) { s -> ArrayList() }
            updateChatUserListItemsForCategories(chatUser)
        }
    }

    /**
     * Adds and removes chat user items from the chat user list depending on the user's categories. For instance, if the
     * user is a moderator, he'll be added to the moderator category (if missing) and if he's no longer a friend, he will
     * be removed from the friends category.
     */
    private fun updateChatUserListItemsForCategories(chatUser: ChatChannelUser) {
        val chatUserCategories = chatUser.chatUserCategories

        Arrays.stream(ChatUserCategory.values())
                .filter { chatUserCategory -> !chatUserCategories.contains(chatUserCategory) }
                .map<List<CategoryOrChatUserListItem>>(Function<ChatUserCategory, List<CategoryOrChatUserListItem>> { categoriesToUserListItems[it] })
                .flatMap<CategoryOrChatUserListItem>(Function<List<CategoryOrChatUserListItem>, Stream<out CategoryOrChatUserListItem>> { it.stream() })
                .filter { categoryOrChatUserListItem -> categoryOrChatUserListItem.getUser() != null && categoryOrChatUserListItem.getUser().equals(chatUser) }
                .forEach(Consumer<CategoryOrChatUserListItem> { chatUserListItems.remove(it) })

        val listItem = CategoryOrChatUserListItem(null, chatUser)
        chatUserCategories.stream()
                .filter { chatUserCategory -> !categoriesToUserListItems[chatUserCategory].contains(listItem) }
                .forEach { chatUserCategory -> addToTreeItemSorted(categoriesToCategoryListItems[chatUserCategory], listItem) }
    }

    private fun addToTreeItemSorted(parent: CategoryOrChatUserListItem, child: CategoryOrChatUserListItem) {
        Platform.runLater {
            categoriesToUserListItems[parent.getCategory()].add(child)
            for (index in chatUserListItems.indexOf(parent) + 1 until chatUserListItems.size) {
                val otherItem = chatUserListItems[index]

                if (otherItem.getCategory() != null || CHAT_USER_ITEM_COMPARATOR.compare(child, otherItem) > 0) {
                    chatUserListItems.add(index, child)
                    return@Platform.runLater
                }
            }
            chatUserListItems.add(child)
        }
    }

    private fun updateCssClass(chatUser: ChatChannelUser) {
        Platform.runLater {
            if (chatUser.player.isPresent) {
                removeUserMessageClass(chatUser, AbstractChatTabController.CSS_CLASS_CHAT_ONLY)
            } else {
                addUserMessageClass(chatUser, AbstractChatTabController.CSS_CLASS_CHAT_ONLY)
            }
            if (chatUser.isModerator) {
                addUserMessageClass(chatUser, CSS_CLASS_MODERATOR)
            } else {
                removeUserMessageClass(chatUser, CSS_CLASS_MODERATOR)
            }
        }
    }

    private fun createWeakColorPropertyListener(chatUser: ChatChannelUser): WeakChangeListener<Color> {
        val listener = { observable, oldValue, newValue -> updateUserMessageColor(chatUser) }

        (colorPropertyListeners as java.util.Map<String, Collection<ChangeListener<Color>>>).computeIfAbsent(chatUser.username) { i -> ArrayList() }.add(listener)
        return WeakChangeListener(listener)
    }

    private fun createWeakSocialStatusListener(chatPrefs: ChatPrefs, chatUser: ChatChannelUser, player: Player): WeakChangeListener<SocialStatus> {
        val listener = { observable, oldValue, newValue ->
            removeUserMessageClass(chatUser, oldValue.cssClass)
            addUserMessageClass(chatUser, newValue.cssClass)

            if (chatPrefs.hideFoeMessages && newValue == FOE) {
                updateUserMessageDisplay(chatUser, "none")
            } else {
                updateUserMessageDisplay(chatUser, "")
            }
            updateChatUserListItemsForCategories(chatUser)
        }
        (socialStatusMessagesListeners as java.util.Map<String, Collection<ChangeListener<SocialStatus>>>).computeIfAbsent(player.username) { i -> ArrayList() }.add(listener)
        return WeakChangeListener(listener)
    }

    private fun createWeakHideFoeMessagesListener(chatUser: ChatChannelUser): ChangeListener<Boolean> {
        val listener = { observable, oldValue, newValue ->
            if (newValue!! && chatUser.player.isPresent && chatUser.player.get().socialStatus == FOE) {
                updateUserMessageDisplay(chatUser, "none")
            } else {
                updateUserMessageDisplay(chatUser, "")
            }
        }
        (hideFoeMessagesListeners as java.util.Map<String, Collection<ChangeListener<Boolean>>>).computeIfAbsent(chatUser.username) { i -> ArrayList() }.add(listener)
        return WeakChangeListener(listener)
    }

    private fun onUserLeft(username: String) {
        Platform.runLater {
            val items = userNamesToListItems[username]
            chatUserListItems.removeAll(items)

            hideFoeMessagesListeners.remove(username)
            socialStatusMessagesListeners.remove(username)
            colorPropertyListeners.remove(username)
        }
    }

    // FIXME use this again
    //  private void updateRandomColorsAllowed(ChatUserHeader parent, ChatChannelUser chatUser, ChatUserItemController chatUserItemController) {
    //    chatUserItemController.setRandomColorAllowed(
    //        (parent == othersTreeItem || parent == chatOnlyTreeItem)
    //            && chatUser.getPlayer().isPresent()
    //            && chatUser.getPlayer().get().getSocialStatus() != SELF
    //    );
    //  }

    fun onKeyReleased(event: KeyEvent) {
        if (event.code == KeyCode.ESCAPE) {
            onSearchFieldClose()
        } else if (event.isControlDown && event.code == KeyCode.F) {
            searchField!!.clear()
            searchField!!.isVisible = !searchField!!.isVisible
            searchField!!.requestFocus()
        }
    }

    fun onSearchFieldClose() {
        searchField!!.isVisible = false
        searchField!!.clear()
    }

    private fun addSearchFieldListener() {
        searchField!!.textProperty().addListener { observable, oldValue, newValue ->
            if (newValue.trim { it <= ' ' }.isEmpty()) {
                jsObject.call("removeHighlight")
            } else {
                jsObject.call("highlightText", newValue)
            }
        }
    }

    fun onAdvancedUserFilter(actionEvent: ActionEvent) {
        advancedUserFilter!!.isSelected = userFilterController!!.isFilterApplied
        if (filterUserPopup!!.isShowing) {
            filterUserPopup!!.hide()
            return
        }

        val button = actionEvent.source as ToggleButton

        val screenBounds = advancedUserFilter!!.localToScreen(advancedUserFilter!!.boundsInLocal)
        filterUserPopup!!.show(button.scene.window, screenBounds.minX, screenBounds.maxY)
    }

    public override fun getInlineStyle(username: String): String {
        val chatUser = chatService.getChatUser(username, channel!!.name)

        val playerOptional = playerService.getPlayerForUsername(username)

        val chatPrefs = preferencesService.preferences!!.chat
        var color = ""
        var display = ""

        if (chatPrefs.hideFoeMessages && playerOptional.isPresent && playerOptional.get().socialStatus == FOE) {
            display = "display: none;"
        } else {
            val chatColorMode = chatPrefs.chatColorMode
            if ((chatColorMode == ChatColorMode.CUSTOM || chatColorMode == ChatColorMode.RANDOM) && chatUser.color != null) {
                color = createInlineStyleFromColor(chatUser.color)
            }
        }

        return String.format("%s%s", color, display)
    }

    internal fun setUserFilter(predicate: Predicate<CategoryOrChatUserListItem>) {
        filteredChatUserList.setPredicate(predicate)
    }

    @Subscribe
    fun onPlayerOnline(event: PlayerOnlineEvent) {
        // We could add a listener on chatChannelUser.playerProperty() but this would result in thousands of mostly idle
        // listeners which we're trying to avoid.
        val chatUser = chatService.getChatUser(event.getPlayer().getUsername(), channel!!.name)
        associateChatUserWithPlayer(event.getPlayer(), chatUser)
    }

    companion object {

        private val USER_CSS_CLASS_FORMAT = "user-%s"

        private val CHAT_USER_ITEM_COMPARATOR = { o1, o2 ->
            val left = o1.getUser()
            val right = o2.getUser()

            Assert.state(left != null, "Only users must be compared")
            Assert.state(right != null, "Only users must be compared")

            if (isSelf(left)) {
                return 1
            }
            if (isSelf(right)) {
                return -1
            }
            right.getUsername().compareTo(left.getUsername(), ignoreCase = true)
        }

        @VisibleForTesting
        internal val CSS_CLASS_MODERATOR = "moderator"

        private fun isSelf(chatUser: ChatChannelUser): Boolean {
            return chatUser.player.isPresent && chatUser.player.get().socialStatus == SocialStatus.SELF
        }
    }
}
