package com.faforever.client.chat

import com.faforever.client.chat.event.ChatMessageEvent
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.net.ConnectionState
import com.faforever.client.theme.UiService
import com.faforever.client.user.UserService
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.util.ProgrammingError
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.HashMap
import java.util.Optional

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ChatController(private val chatService: ChatService, private val uiService: UiService, private val userService: UserService, private val eventBus: EventBus) : AbstractViewController<Node>() {

    private val nameToChatTabController: MutableMap<String, AbstractChatTabController>
    override var root: Node? = null
    var tabPane: TabPane? = null
    var connectingProgressPane: Pane? = null
    var noOpenTabsContainer: VBox? = null
    var channelNameTextField: TextField? = null

    init {

        nameToChatTabController = HashMap()
    }

    private fun onChannelLeft(channel: Channel) {
        Platform.runLater { removeTab(channel.name) }
    }

    private fun onChannelJoined(channel: Channel) {
        val channelName = channel.name
        chatService.addUsersListener(channelName) { change ->
            if (change.wasRemoved()) {
                onChatUserLeftChannel(channelName, change.valueRemoved.username)
            }
            if (change.wasAdded()) {
                onUserJoinedChannel(change.valueAdded, channelName)
            }
        }
    }

    private fun onDisconnected() {
        Platform.runLater {
            connectingProgressPane!!.isVisible = true
            tabPane!!.isVisible = false
            noOpenTabsContainer!!.isVisible = false
        }
    }

    private fun onConnected() {
        Platform.runLater {
            connectingProgressPane!!.isVisible = false
            tabPane!!.isVisible = true
            noOpenTabsContainer!!.isVisible = false
        }
    }

    private fun onConnecting() {
        Platform.runLater {
            connectingProgressPane!!.isVisible = true
            tabPane!!.isVisible = false
            noOpenTabsContainer!!.isVisible = false
        }
    }

    private fun onLoggedOut() {
        Platform.runLater { tabPane!!.tabs.clear() }
    }

    private fun removeTab(playerOrChannelName: String) {
        val controller = nameToChatTabController[playerOrChannelName]
        if (controller != null) {
            tabPane!!.tabs.remove(controller.root)
        }
    }

    private fun getOrCreateChannelTab(channelName: String): AbstractChatTabController {
        JavaFxUtil.assertApplicationThread()
        if (!nameToChatTabController.containsKey(channelName)) {
            val tab = uiService.loadFxml<ChannelTabController>("theme/chat/channel_tab.fxml")
            tab.setChannel(chatService.getOrCreateChannel(channelName))
            addTab(channelName, tab)
        }
        return nameToChatTabController[channelName]
    }

    private fun addTab(playerOrChannelName: String, tabController: AbstractChatTabController) {
        JavaFxUtil.assertApplicationThread()
        nameToChatTabController[playerOrChannelName] = tabController
        val tab = tabController.root

        if (chatService.isDefaultChannel(playerOrChannelName)) {
            tabPane!!.tabs.add(0, tab)
            tabPane!!.selectionModel.select(tab)
        } else {
            tabPane!!.tabs.add(tab)
        }
    }

    override fun initialize() {
        super.initialize()
        eventBus.register(this)

        tabPane!!.tabs.addListener({ observable -> noOpenTabsContainer!!.isVisible = tabPane!!.tabs.isEmpty() } as InvalidationListener)

        chatService.addChannelsListener { change ->
            if (change.wasRemoved()) {
                onChannelLeft(change.valueRemoved)
            }
            if (change.wasAdded()) {
                onChannelJoined(change.valueAdded)
            }
        }

        JavaFxUtil.addListener(chatService.connectionStateProperty()) { observable, oldValue, newValue -> onConnectionStateChange(newValue) }
        onConnectionStateChange(chatService.connectionStateProperty().get())

        JavaFxUtil.addListener(tabPane!!.tabs, { change ->
            while (change.next()) {
                change.getRemoved().forEach { tab -> nameToChatTabController.remove(tab.getId()) }
            }
        } as ListChangeListener<Tab>)
    }

    @Subscribe
    fun onLoggedOutEvent(event: LoggedOutEvent) {
        onLoggedOut()
    }

    private fun onConnectionStateChange(newValue: ConnectionState) {
        when (newValue) {
            ConnectionState.DISCONNECTED -> onDisconnected()
            ConnectionState.CONNECTED -> {
            }
            ConnectionState.CONNECTING -> onConnecting()
            else -> throw ProgrammingError("Uncovered connection state: $newValue")
        }//        onConnected();
    }

    @Subscribe
    fun onChatMessage(event: ChatMessageEvent) {
        Platform.runLater {
            val message = event.message
            if (!message.isPrivate) {
                getOrCreateChannelTab(message.getSource()).onChatMessage(message)
            } else {
                addAndGetPrivateMessageTab(message.getSource()).onChatMessage(message)
            }
        }
    }

    private fun addAndGetPrivateMessageTab(username: String): AbstractChatTabController {
        JavaFxUtil.assertApplicationThread()
        if (!nameToChatTabController.containsKey(username)) {
            val tab = uiService.loadFxml<PrivateChatTabController>("theme/chat/private_chat_tab.fxml")
            tab.receiver = username
            addTab(username, tab)
        }

        return nameToChatTabController[username]
    }

    @Subscribe
    fun onInitiatePrivateChatEvent(event: InitiatePrivateChatEvent) {
        Platform.runLater { openPrivateMessageTabForUser(event.username) }
    }

    private fun openPrivateMessageTabForUser(username: String) {
        if (username.equals(userService.username, ignoreCase = true)) {
            return
        }
        val controller = addAndGetPrivateMessageTab(username)
        tabPane!!.selectionModel.select(controller.root)
    }

    fun onJoinChannelButtonClicked() {
        val channelName = channelNameTextField!!.text
        channelNameTextField!!.clear()

        joinChannel(channelName)
    }

    private fun joinChannel(channelName: String) {
        chatService.joinChannel(channelName)
    }

    private fun onChatUserLeftChannel(channelName: String, username: String) {
        if (!username.equals(userService.username, ignoreCase = true)) {
            return
        }
        val chatTab = nameToChatTabController[channelName]
        if (chatTab != null) {
            Platform.runLater { tabPane!!.tabs.remove(chatTab.root) }
        }
    }

    private fun onUserJoinedChannel(chatUser: ChatChannelUser, channelName: String) {
        Platform.runLater {
            if (isCurrentUser(chatUser)) {
                val tabController = getOrCreateChannelTab(channelName)
                onConnected()
                if (channelName == chatService.defaultChannelName) {
                    tabPane!!.selectionModel.select(tabController.root)
                }
            }
        }
    }

    private fun isCurrentUser(chatUser: ChatChannelUser): Boolean {
        return chatUser.username.equals(userService.username, ignoreCase = true)
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (!tabPane!!.tabs.isEmpty()) {
            val tab = tabPane!!.selectionModel.selectedItem
            nameToChatTabController[tab.id].onDisplay()
        }
    }

    override fun onHide() {
        super.onHide()
        if (!tabPane!!.tabs.isEmpty()) {
            val tab = tabPane!!.selectionModel.selectedItem
            Optional.ofNullable(nameToChatTabController[tab.id]).ifPresent(Consumer<AbstractChatTabController> { it.onHide() })
        }
    }
}
