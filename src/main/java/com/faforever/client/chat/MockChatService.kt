package com.faforever.client.chat

import com.faforever.client.FafClientApplication
import com.faforever.client.i18n.I18n
import com.faforever.client.net.ConnectionState
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import com.faforever.client.user.UserService
import com.faforever.client.user.event.LoginSuccessEvent
import com.faforever.client.util.ConcurrentUtil
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.MapChangeListener
import javafx.concurrent.Task
import javafx.scene.paint.Color
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import com.faforever.client.task.CompletableTask.Priority.HIGH

@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
// NOSONAR
class MockChatService @Inject
constructor(private val userService: UserService, private val taskService: TaskService, private val i18n: I18n, private val eventBus: EventBus) : ChatService {
    private val timer: Timer
    private val onChatMessageListeners: Collection<Consumer<ChatMessage>>
    private val channelUserListListeners: MutableMap<String, Channel>
    private val connectionState: ObjectProperty<ConnectionState>
    private val unreadMessagesCount: IntegerProperty

    override val defaultChannelName: String
        get() = channelUserListListeners.keys.iterator().next()

    init {
        connectionState = SimpleObjectProperty(ConnectionState.DISCONNECTED)
        unreadMessagesCount = SimpleIntegerProperty()

        onChatMessageListeners = ArrayList()
        channelUserListListeners = HashMap()

        timer = Timer(true)
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onLoginSuccessEvent(event: LoginSuccessEvent) {
        connect()
    }

    private fun simulateConnectionEstablished() {
        connectionState.set(ConnectionState.CONNECTED)
        joinChannel("#mockChannel")
    }

    override fun connect() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                simulateConnectionEstablished()
            }
        }, CONNECTION_DELAY)
    }

    override fun disconnect() {
        timer.cancel()
    }

    override fun sendMessageInBackground(target: String, message: String): CompletableFuture<String> {
        return taskService.submitTask<>(object : CompletableTask<String>(HIGH) {
            @Throws(Exception::class)
            override fun call(): String {
                updateTitle(i18n.get("chat.sendMessageTask.title"))

                Thread.sleep(200)
                return message
            }
        }).future
    }

    override fun getOrCreateChannel(channelName: String): Channel {
        (channelUserListListeners as java.util.Map<String, Channel>).putIfAbsent(channelName, Channel(channelName))
        return channelUserListListeners[channelName]
    }

    override fun getOrCreateChatUser(username: String, channel: String, isModerator: Boolean): ChatChannelUser? {
        return null
    }

    override fun addUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>) {
        getOrCreateChannel(channelName).addUsersListeners(listener)
    }

    override fun addChatUsersByNameListener(listener: MapChangeListener<String, ChatChannelUser>) {

    }

    override fun addChannelsListener(listener: MapChangeListener<String, Channel>) {

    }

    override fun removeUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>) {

    }

    override fun leaveChannel(channelName: String) {

    }

    override fun sendActionInBackground(target: String, action: String): CompletableFuture<String> {
        return sendMessageInBackground(target, action)
    }

    override fun joinChannel(channelName: String) {
        ConcurrentUtil.executeInBackground(object : Task<Void>() {
            @Throws(Exception::class)
            override fun call(): Void? {
                val chatUser = ChatChannelUser(userService.username, null, false)
                val mockUser = ChatChannelUser("MockUser", null, false)
                val moderatorUser = ChatChannelUser("MockModerator", null, true)

                val channel = getOrCreateChannel(channelName)
                channel.addUser(chatUser)
                channel.addUser(mockUser)
                channel.addUser(moderatorUser)
                channel.topic = "le wild channel topic appears"

                return null
            }
        })

        timer.schedule(object : TimerTask() {
            override fun run() {
                for (onChatMessageListener in onChatMessageListeners) {
                    val chatMessage = ChatMessage(channelName, Instant.now(), "Mock User",
                            String.format(
                                    "%1\$s Lorem ipsum dolor sit amet, consetetur %1\$s sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam %1\$s " + "http://www.faforever.com/wp-content/uploads/2013/07/cropped-backForum41.jpg",
                                    userService.username
                            )
                    )

                    onChatMessageListener.accept(chatMessage)
                }
            }
        }, 0, CHAT_MESSAGE_INTERVAL.toLong())
    }

    override fun isDefaultChannel(channelName: String): Boolean {
        return true
    }

    override fun close() {

    }

    override fun connectionStateProperty(): ObjectProperty<ConnectionState> {
        return connectionState
    }

    override fun reconnect() {

    }

    override fun whois(username: String) {

    }

    override fun incrementUnreadMessagesCount(delta: Int) {
        synchronized(unreadMessagesCount) {
            unreadMessagesCount.set(unreadMessagesCount.get() + delta)
        }
    }

    override fun unreadMessagesCount(): ReadOnlyIntegerProperty {
        return unreadMessagesCount
    }

    override fun getChatUser(username: String, channelName: String): ChatChannelUser {
        return ChatChannelUser(username, Color.ALICEBLUE, false)
    }

    companion object {

        private val CHAT_MESSAGE_INTERVAL = 5000
        private val CONNECTION_DELAY: Long = 1000
    }
}
