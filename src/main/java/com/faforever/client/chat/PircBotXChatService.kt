package com.faforever.client.chat

import com.faforever.client.FafClientApplication
import com.faforever.client.chat.event.ChatMessageEvent
import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.Irc
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.net.ConnectionState
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerOnlineEvent
import com.faforever.client.player.SocialStatus
import com.faforever.client.player.UserOfflineEvent
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.remote.domain.SocialMessage
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent
import com.faforever.client.user.UserService
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableSortedSet
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.common.hash.Hashing
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.concurrent.Task
import javafx.scene.paint.Color
import lombok.extern.slf4j.Slf4j
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.User
import org.pircbotx.UserHostmask
import org.pircbotx.UserLevel
import org.pircbotx.UtilSSLSocketFactory
import org.pircbotx.delay.StaticDelay
import org.pircbotx.exception.IrcException
import org.pircbotx.hooks.Event
import org.pircbotx.hooks.events.ActionEvent
import org.pircbotx.hooks.events.ConnectEvent
import org.pircbotx.hooks.events.DisconnectEvent
import org.pircbotx.hooks.events.JoinEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.events.MotdEvent
import org.pircbotx.hooks.events.NoticeEvent
import org.pircbotx.hooks.events.OpEvent
import org.pircbotx.hooks.events.PartEvent
import org.pircbotx.hooks.events.PrivateMessageEvent
import org.pircbotx.hooks.events.QuitEvent
import org.pircbotx.hooks.events.TopicEvent
import org.pircbotx.hooks.events.UserListEvent
import org.pircbotx.hooks.types.GenericEvent
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Inject
import java.io.IOException
import java.time.Instant
import java.util.ArrayList
import java.util.Arrays
import java.util.Objects
import java.util.Optional
import java.util.TreeMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.stream.Collectors

import com.faforever.client.chat.ChatColorMode.CUSTOM
import com.faforever.client.chat.ChatColorMode.RANDOM
import com.faforever.client.task.CompletableTask.Priority.HIGH
import java.lang.String.format
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale.US
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.FXCollections.observableMap
import org.apache.commons.lang3.StringUtils.containsIgnoreCase

@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
class PircBotXChatService @Inject
constructor(private val preferencesService: PreferencesService, private val userService: UserService, private val taskService: TaskService,
            private val fafService: FafService, private val i18n: I18n, private val pircBotXFactory: PircBotXFactory,
            private val threadPoolExecutor: ThreadPoolExecutor,
            private val eventBus: EventBus,
            clientProperties: ClientProperties) : ChatService {
    @VisibleForTesting
    internal val connectionState: ObjectProperty<ConnectionState>
    private val eventListeners: MutableMap<Class<out GenericEvent>, ArrayList<ChatEventListener<*>>>
    /**
     * Maps channels by name.
     */
    private val channels: ObservableMap<String, Channel>
    /** Key is the result of [.mapKey].  */
    private val chatChannelUsersByChannelAndName: ObservableMap<String, ChatChannelUser>
    private val unreadMessagesCount: SimpleIntegerProperty
    private val ircHost: String
    private val ircPort: Int
    override val defaultChannelName: String
    private val reconnectDelay: Int

    private var configuration: Configuration? = null
    private var pircBotX: PircBotX? = null
    /** Called when the IRC server has confirmed our identity.  */
    private var identifiedFuture: CompletableFuture<Void>? = null
    private var connectionTask: Task<Void>? = null
    /**
     * A list of channels the server wants us to join.
     */
    private var autoChannels: MutableList<String>? = null
    /**
     * Indicates whether the "auto channels" already have been joined. This is needed because we don't want to auto join
     * channels after a reconnect that the user left before the reconnect.
     */
    private var autoChannelsJoined: Boolean = false

    private val password: String
        get() = Hashing.md5().hashString(Hashing.sha256().hashString(userService.password!!, UTF_8).toString(), UTF_8).toString()

    init {

        val irc = clientProperties.getIrc()
        this.ircHost = irc.getHost()
        this.ircPort = irc.getPort()
        this.defaultChannelName = irc.getDefaultChannel()
        this.reconnectDelay = irc.getReconnectDelay()

        connectionState = SimpleObjectProperty(ConnectionState.DISCONNECTED)
        eventListeners = ConcurrentHashMap<Class<out GenericEvent>, ArrayList<ChatEventListener>>()
        channels = observableHashMap()
        chatChannelUsersByChannelAndName = observableMap(TreeMap(String.CASE_INSENSITIVE_ORDER))
        unreadMessagesCount = SimpleIntegerProperty()
        identifiedFuture = CompletableFuture()
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
        fafService.addOnMessageListener(SocialMessage::class.java, Consumer<SocialMessage> { this.onSocialMessage(it) })
        connectionState.addListener { observable, oldValue, newValue ->
            when (newValue) {
                ConnectionState.DISCONNECTED, ConnectionState.CONNECTING -> onDisconnected()
            }
        }

        addEventListener(NoticeEvent::class.java, ChatEventListener<NoticeEvent> { this.onNotice(it) })
        addEventListener(ConnectEvent::class.java, { event -> connectionState.set(ConnectionState.CONNECTED) })
        addEventListener(DisconnectEvent::class.java, { event -> connectionState.set(ConnectionState.DISCONNECTED) })
        addEventListener(UserListEvent::class.java, { event -> onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers(), event.getChannel().getName())) })
        addEventListener(JoinEvent::class.java, ChatEventListener<JoinEvent> { this.onJoinEvent(it) })
        addEventListener(PartEvent::class.java, { event -> onChatUserLeftChannel(event.getChannel().getName(), event.getUser().getNick()) })
        addEventListener(QuitEvent::class.java, { event -> onChatUserQuit(event.getUser().getNick()) })
        addEventListener(TopicEvent::class.java, { event -> getOrCreateChannel(event.getChannel().getName()).topic = event.getTopic() })
        addEventListener(MessageEvent::class.java, ChatEventListener<MessageEvent> { this.onMessage(it) })
        addEventListener(ActionEvent::class.java, ChatEventListener<ActionEvent> { this.onAction(it) })
        addEventListener(PrivateMessageEvent::class.java, ChatEventListener<PrivateMessageEvent> { this.onPrivateMessage(it) })
        addEventListener(MotdEvent::class.java, ChatEventListener<MotdEvent> { this.onMotd(it) })
        addEventListener(OpEvent::class.java, ChatEventListener<OpEvent> { this.onOp(it) })

        val chatPrefs = preferencesService.preferences!!.chat
        JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
                { change -> preferencesService.store() } as MapChangeListener<in String, in Color>
        )
        JavaFxUtil.addListener(chatPrefs.chatColorModeProperty()) { observable, oldValue, newValue ->
            synchronized(chatChannelUsersByChannelAndName) {
                when (newValue) {
                    CUSTOM -> chatChannelUsersByChannelAndName.values.stream()
                            .filter { chatUser -> chatPrefs.userToColor.containsKey(userToColorKey(chatUser.username)) }
                            .forEach { chatUser -> chatUser.color = chatPrefs.userToColor[userToColorKey(chatUser.username)] }

                    RANDOM -> for (chatUser in chatChannelUsersByChannelAndName.values) {
                        chatUser.color = ColorGeneratorUtil.generateRandomColor(chatUser.username.hashCode().toLong())
                    }

                    else -> for (chatUser in chatChannelUsersByChannelAndName.values) {
                        chatUser.color = null
                    }
                }
            }
        }
    }

    private fun onOp(event: OpEvent) {
        val recipient = event.recipient
        if (recipient != null) {
            onModeratorSet(event.channel.name, recipient.nick)
        }
    }

    private fun userToColorKey(username: String): String {
        return username.toLowerCase(US)
    }

    private fun onJoinEvent(event: JoinEvent) {
        val user = Objects.requireNonNull<User>(event.user)
        log.debug("User joined channel: {}", user)
        onJoinEvent(event.channel.name, getOrCreateChatUser(user, event.channel.name))
    }

    private fun getOrCreateChatUser(user: User, channelName: String): ChatChannelUser? {
        val username = if (user.nick != null) user.nick else user.login

        val isModerator = user.channels.stream()
                .filter { channel -> channel.name == channelName }
                .flatMap { channel -> user.getUserLevels(channel).stream() }
                .anyMatch(Predicate<UserLevel> { MODERATOR_USER_LEVELS.contains(it) })

        return getOrCreateChatUser(username, channelName, isModerator)
    }

    private fun onMotd(event: MotdEvent) {
        sendIdentify(event.getBot<PircBotX>().configuration)
    }

    @Subscribe
    fun onLoginSuccessEvent(event: LoginSuccessEvent) {
        connect()
    }

    @Subscribe
    fun onLoggedOutEvent(event: LoggedOutEvent) {
        disconnect()
        eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0))
    }

    private fun onNotice(event: NoticeEvent) {
        val config = event.getBot<PircBotX>().configuration
        val hostmask = event.userHostmask

        if (config.nickservOnSuccess != null && containsIgnoreCase(hostmask.hostmask, config.nickservNick)) {
            val message = event.message
            if (containsIgnoreCase(message, config.nickservOnSuccess) || containsIgnoreCase(message, "registered under your account")) {
                onIdentified()
            } else if (message.contains("isn't registered")) {
                pircBotX!!.sendIRC().message(config.nickservNick, format("register %s %s@users.faforever.com", password, userService.username))
            } else if (message.contains(" registered")) {
                // We just registered and are now identified
                onIdentified()
            } else if (message.contains("choose a different nick")) {
                // The server didn't accept our IDENTIFY command, well then, let's send a private message to nickserv manually
                sendIdentify(config)
            }
        }
    }

    private fun sendIdentify(config: Configuration) {
        pircBotX!!.sendIRC().message(config.nickservNick, format("identify %s", password))
    }

    private fun onIdentified() {
        identifiedFuture!!.thenAccept { aVoid ->
            if (!autoChannelsJoined) {
                joinAutoChannels()
            } else {
                synchronized(channels) {
                    log.debug("Joining all channels: {}", channels)
                    channels.keys.forEach(Consumer<String> { this.joinChannel(it) })
                }
            }
        }
        identifiedFuture!!.complete(null)
    }

    private fun joinAutoChannels() {
        log.debug("Joining auto channel: {}", autoChannels)
        if (autoChannels == null) {
            return
        }
        autoChannels!!.forEach(Consumer<String> { this.joinChannel(it) })
        autoChannelsJoined = true
    }

    private fun onDisconnected() {
        autoChannelsJoined = false
        synchronized(channels) {
            channels.values.forEach(Consumer<Channel> { it.clearUsers() })
        }
    }

    private fun <T : GenericEvent> addEventListener(eventClass: Class<T>, listener: ChatEventListener<T>) {
        (eventListeners as java.util.Map<Class<out GenericEvent>, ArrayList<ChatEventListener>>).computeIfAbsent(eventClass) { aClass -> ArrayList<ChatEventListener>() }.add(listener)
    }

    private fun onChatUserList(channelName: String, users: List<ChatChannelUser>) {
        getOrCreateChannel(channelName).addUsers(users)
    }

    private fun chatUsers(users: ImmutableSortedSet<User>, channel: String): List<ChatChannelUser> {
        return users.stream().map<ChatChannelUser> { user -> getOrCreateChatUser(user, channel) }.collect<List<ChatChannelUser>, Any>(Collectors.toList())
    }

    private fun onJoinEvent(channelName: String, chatUser: ChatChannelUser?) {
        getOrCreateChannel(channelName).addUser(chatUser)
    }

    private fun onChatUserLeftChannel(channelName: String, username: String) {
        if (getOrCreateChannel(channelName).removeUser(username) == null) {
            return
        }
        log.debug("User '{}' left channel: {}", username, channelName)
        if (userService.username.equals(username, ignoreCase = true)) {
            synchronized(channels) {
                channels.remove(channelName)
            }
        }
        synchronized(chatChannelUsersByChannelAndName) {
            chatChannelUsersByChannelAndName.remove(mapKey(username, channelName))
        }
        // The server doesn't yet tell us when a user goes offline, so we have to rely on the user leaving IRC.
        if (defaultChannelName == channelName) {
            eventBus.post(UserOfflineEvent(username))
        }
    }

    private fun onChatUserQuit(username: String) {
        synchronized(channels) {
            channels.values.forEach { channel -> onChatUserLeftChannel(channel.name, username) }
        }
    }

    private fun onModeratorSet(channelName: String, username: String) {
        getOrCreateChannel(channelName).addModerator(username)
    }

    private fun init() {
        val username = userService.username

        configuration = Configuration.Builder()
                .setName(username)
                .setLogin(userService.userId.toString())
                .setRealName(username)
                .addServer(ircHost, ircPort)
                .setSocketFactory(UtilSSLSocketFactory().trustAllCertificates())
                .setAutoSplitMessage(true)
                .setEncoding(UTF_8)
                .addListener(Listener { this.onEvent(it) })
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setMessageDelay(StaticDelay(0))
                .setAutoReconnectDelay(StaticDelay(reconnectDelay.toLong()))
                .setNickservPassword(password)
                .setAutoReconnect(true)
                .buildConfiguration()

        pircBotX = pircBotXFactory.createPircBotX(configuration)
    }

    private fun onSocialMessage(socialMessage: SocialMessage) {
        if (!autoChannelsJoined && socialMessage.channels != null) {
            this.autoChannels = ArrayList(socialMessage.channels!!)
            autoChannels!!.remove(defaultChannelName)
            autoChannels!!.add(0, defaultChannelName)
            threadPoolExecutor.execute { this.joinAutoChannels() }
        }
    }

    private fun onEvent(event: Event) {
        if (!eventListeners.containsKey(event.javaClass)) {
            return
        }
        eventListeners[event.javaClass].forEach { listener -> listener.onEvent(event) }
    }

    private fun onAction(event: ActionEvent) {
        val user = event.user
        if (user == null) {
            log.warn("Action event without user: {}", event)
            return
        }

        val source: String
        val channel = event.channel
        if (channel == null) {
            source = user.nick
        } else {
            source = channel.name
        }
        eventBus.post(ChatMessageEvent(ChatMessage(source, Instant.ofEpochMilli(event.timestamp), user.nick, event.message, true)))
    }

    private fun onMessage(event: MessageEvent) {
        val user = event.user
        if (user == null) {
            log.warn("Action event without user: {}", event)
            return
        }

        val source: String
        val channel = event.channel
        source = channel.name

        eventBus.post(ChatMessageEvent(ChatMessage(source, Instant.ofEpochMilli(event.timestamp), user.nick, event.message, false)))
    }

    private fun onPrivateMessage(event: PrivateMessageEvent) {
        val user = event.user
        if (user == null) {
            log.warn("Private message without user: {}", event)
            return
        }
        log.debug("Received private message: {}", event)

        val sender = getOrCreateChatUser(user.nick, event.user!!.nick, false)
        if (sender != null
                && sender.player.isPresent
                && sender.player.get().socialStatus == SocialStatus.FOE
                && preferencesService.preferences!!.chat.hideFoeMessages) {
            log.debug("Suppressing chat message from foe '{}'", user.nick)
            return
        }
        eventBus.post(ChatMessageEvent(ChatMessage(user.nick, Instant.ofEpochMilli(event.timestamp), user.nick, event.message)))
    }

    override fun connect() {
        init()

        connectionTask = object : Task<Void>() {
            override fun call(): Void? {
                while (!isCancelled) {
                    try {
                        connectionState.set(ConnectionState.CONNECTING)
                        val server = configuration!!.servers[0]
                        log.info("Connecting to IRC at {}:{}", server.hostname, server.port)
                        pircBotX!!.startBot()
                    } catch (e: IOException) {
                        connectionState.set(ConnectionState.DISCONNECTED)
                    } catch (e: IrcException) {
                        connectionState.set(ConnectionState.DISCONNECTED)
                    } catch (e: RuntimeException) {
                        connectionState.set(ConnectionState.DISCONNECTED)
                    }

                }
                return null
            }
        }
        threadPoolExecutor.execute(connectionTask!!)
    }

    override fun disconnect() {
        log.info("Disconnecting from IRC")
        if (connectionTask != null) {
            connectionTask!!.cancel(false)
        }
        if (pircBotX!!.isConnected) {
            pircBotX!!.stopBotReconnect()
            pircBotX!!.sendIRC().quitServer()
            synchronized(channels) {
                channels.clear()
            }
        }
        identifiedFuture = CompletableFuture()
    }

    override fun sendMessageInBackground(target: String, message: String): CompletableFuture<String> {
        eventBus.post(ChatMessageEvent(ChatMessage(target, Instant.now(), userService.username, message)))
        return taskService.submitTask<>(object : CompletableTask<String>(HIGH) {
            override fun call(): String {
                updateTitle(i18n.get("chat.sendMessageTask.title"))
                pircBotX!!.sendIRC().message(target, message)
                return message
            }
        }).future
    }

    override fun getOrCreateChannel(channelName: String): Channel {
        synchronized(channels) {
            if (!channels.containsKey(channelName)) {
                channels[channelName] = Channel(channelName)
            }
            return channels[channelName]
        }
    }

    override fun getOrCreateChatUser(username: String, channel: String, isModerator: Boolean): ChatChannelUser? {
        synchronized(chatChannelUsersByChannelAndName) {
            val key = mapKey(username, channel)
            if (!chatChannelUsersByChannelAndName.containsKey(key)) {
                val chatPrefs = preferencesService.preferences!!.chat
                var color: Color? = null

                if (chatPrefs.chatColorMode == CUSTOM && chatPrefs.userToColor.containsKey(userToColorKey(username))) {
                    color = chatPrefs.userToColor[userToColorKey(username)]
                } else if (chatPrefs.chatColorMode == RANDOM) {
                    color = ColorGeneratorUtil.generateRandomColor(userToColorKey(username).hashCode().toLong())
                }

                val chatChannelUser = ChatChannelUser(username, color, isModerator)
                eventBus.post(ChatUserCreatedEvent(chatChannelUser))
                chatChannelUsersByChannelAndName[key] = chatChannelUser
            }
            return chatChannelUsersByChannelAndName[key]
        }
    }

    override fun addUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>) {
        getOrCreateChannel(channelName).addUsersListeners(listener)
    }

    override fun addChatUsersByNameListener(listener: MapChangeListener<String, ChatChannelUser>) {
        synchronized(chatChannelUsersByChannelAndName) {
            JavaFxUtil.addListener(chatChannelUsersByChannelAndName, listener)
        }
    }

    override fun addChannelsListener(listener: MapChangeListener<String, Channel>) {
        JavaFxUtil.addListener(channels, listener)
    }

    override fun removeUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>) {
        getOrCreateChannel(channelName).removeUserListener(listener)
    }

    override fun leaveChannel(channelName: String) {
        pircBotX!!.userChannelDao.getChannel(channelName).send().part()
    }

    override fun sendActionInBackground(target: String, action: String): CompletableFuture<String> {
        return taskService.submitTask<>(object : CompletableTask<String>(HIGH) {
            override fun call(): String {
                updateTitle(i18n.get("chat.sendActionTask.title"))

                pircBotX!!.sendIRC().action(target, action)
                return action
            }
        }).future
    }

    override fun joinChannel(channelName: String) {
        log.debug("Joining channel (waiting for identification): {}", channelName)
        identifiedFuture!!.thenAccept { aVoid ->
            log.debug("Joining channel: {}", channelName)
            pircBotX!!.sendIRC().joinChannel(channelName)
        }
    }

    override fun isDefaultChannel(channelName: String): Boolean {
        return defaultChannelName == channelName
    }

    @PreDestroy
    override fun close() {
        // TODO clean up disconnect() and close()
        identifiedFuture!!.cancel(false)
        if (connectionTask != null) {
            connectionTask!!.cancel()
        }
        if (pircBotX != null) {
            pircBotX!!.sendIRC().quitServer()
        }
    }

    override fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState> {
        return connectionState
    }

    override fun reconnect() {
        disconnect()
        connect()
    }

    override fun whois(username: String) {
        pircBotX!!.sendIRC().whois(username)
    }

    override fun incrementUnreadMessagesCount(delta: Int) {
        eventBus.post(UpdateApplicationBadgeEvent.ofDelta(delta))
    }

    override fun unreadMessagesCount(): ReadOnlyIntegerProperty {
        return unreadMessagesCount
    }

    override fun getChatUser(username: String, channelName: String): ChatChannelUser {
        return Optional.ofNullable(chatChannelUsersByChannelAndName[mapKey(username, channelName)])
                .orElseThrow { IllegalArgumentException("Chat user '$username' is unknown for channel '$channelName'") }
    }

    private fun mapKey(username: String, channelName: String): String {
        return username + channelName
    }

    @Subscribe
    fun onPlayerOnline(event: PlayerOnlineEvent) {
        val player = event.getPlayer()

        synchronized(channels) {
            val channelUsers = channels.values.stream()
                    .map<ChatChannelUser> { channel -> chatChannelUsersByChannelAndName[mapKey(player.getUsername(), channel.name)] }
                    .filter(Predicate<ChatChannelUser> { Objects.nonNull(it) })
                    .peek { chatChannelUser -> chatChannelUser.setPlayer(player) }
                    .collect<List<ChatChannelUser>, Any>(Collectors.toList())

            player.chatChannelUsers.addAll(channelUsers)
        }
    }

    internal interface ChatEventListener<T> {

        fun onEvent(event: T)
    }

    companion object {

        private val MODERATOR_USER_LEVELS = Arrays.asList(UserLevel.OP, UserLevel.HALFOP, UserLevel.SUPEROP, UserLevel.OWNER)
        private val SOCKET_TIMEOUT = 10000
    }
}
