package com.faforever.client.remote

import com.faforever.client.FafClientApplication
import com.faforever.client.config.CacheNames
import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.Server
import com.faforever.client.fa.relay.GpgClientMessageSerializer
import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.fa.relay.GpgServerMessageType
import com.faforever.client.game.Faction
import com.faforever.client.game.NewGameInfo
import com.faforever.client.i18n.I18n
import com.faforever.client.legacy.UidService
import com.faforever.client.login.LoginFailedException
import com.faforever.client.net.ConnectionState
import com.faforever.client.notification.DismissAction
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.ReportAction
import com.faforever.client.notification.Severity
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.rankedmatch.SearchLadder1v1ClientMessage
import com.faforever.client.rankedmatch.StopSearchLadder1v1ClientMessage
import com.faforever.client.remote.domain.AddFoeMessage
import com.faforever.client.remote.domain.AddFriendMessage
import com.faforever.client.remote.domain.AuthenticationFailedMessage
import com.faforever.client.remote.domain.Avatar
import com.faforever.client.remote.domain.AvatarMessage
import com.faforever.client.remote.domain.ClientMessage
import com.faforever.client.remote.domain.ClientMessageType
import com.faforever.client.remote.domain.FafServerMessageType
import com.faforever.client.remote.domain.GameAccess
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.HostGameMessage
import com.faforever.client.remote.domain.IceServersServerMessage
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer
import com.faforever.client.remote.domain.InitSessionMessage
import com.faforever.client.remote.domain.JoinGameMessage
import com.faforever.client.remote.domain.ListIceServersMessage
import com.faforever.client.remote.domain.ListPersonalAvatarsMessage
import com.faforever.client.remote.domain.LoginClientMessage
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.domain.NoticeMessage
import com.faforever.client.remote.domain.PingMessage
import com.faforever.client.remote.domain.RatingRange
import com.faforever.client.remote.domain.RemoveFoeMessage
import com.faforever.client.remote.domain.RemoveFriendMessage
import com.faforever.client.remote.domain.RestoreGameSessionMessage
import com.faforever.client.remote.domain.SelectAvatarMessage
import com.faforever.client.remote.domain.SerializableMessage
import com.faforever.client.remote.domain.ServerCommand
import com.faforever.client.remote.domain.ServerMessage
import com.faforever.client.remote.domain.SessionMessage
import com.faforever.client.remote.domain.VictoryCondition
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter
import com.faforever.client.remote.gson.GameAccessTypeAdapter
import com.faforever.client.remote.gson.GameStateTypeAdapter
import com.faforever.client.remote.gson.GpgServerMessageTypeTypeAdapter
import com.faforever.client.remote.gson.MessageTargetTypeAdapter
import com.faforever.client.remote.gson.RatingRangeTypeAdapter
import com.faforever.client.remote.gson.ServerMessageTypeAdapter
import com.faforever.client.remote.gson.ServerMessageTypeTypeAdapter
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter
import com.faforever.client.reporting.ReportingService
import com.faforever.client.update.Version
import com.github.nocatch.NoCatch
import com.google.common.annotations.VisibleForTesting
import com.google.common.hash.Hashing
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import org.apache.commons.compress.utils.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils

import javax.annotation.PreDestroy
import javax.inject.Inject
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.net.Socket
import java.net.URL
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import com.faforever.client.util.ConcurrentUtil.executeInBackground
import java.nio.charset.StandardCharsets.UTF_8

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
class FafServerAccessorImpl @Inject
constructor(private val preferencesService: PreferencesService,
            private val uidService: UidService,
            private val notificationService: NotificationService,
            private val i18n: I18n,
            private val clientProperties: ClientProperties,
            private val reportingService: ReportingService) : AbstractServerAccessor(), FafServerAccessor {
    private val gson: Gson
    private val messageListeners: HashMap<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>
    private var fafConnectionTask: Task<Void>? = null
    private var localIp: String? = null
    private var serverWriter: ServerWriter? = null
    private var loginFuture: CompletableFuture<LoginMessage>? = null
    private var sessionFuture: CompletableFuture<SessionMessage>? = null
    private var gameLaunchFuture: CompletableFuture<GameLaunchMessage>? = null
    private val sessionId: ObjectProperty<Long>
    private var username: String? = null
    private var password: String? = null
    private val connectionState: ObjectProperty<ConnectionState>
    private var fafServerSocket: Socket? = null
    private var avatarsFuture: CompletableFuture<List<Avatar>>? = null
    private var iceServersFuture: CompletableFuture<List<IceServer>>? = null

    override val availableAvatars: List<Avatar>
        @Cacheable(CacheNames.AVAILABLE_AVATARS)
        get() {
            avatarsFuture = CompletableFuture()
            writeToServer(ListPersonalAvatarsMessage())
            return NoCatch.noCatch<List<Avatar>> { avatarsFuture!!.get(10, TimeUnit.SECONDS) }
        }

    override val iceServers: CompletableFuture<List<IceServer>>
        get() {
            iceServersFuture = CompletableFuture()
            writeToServer(ListIceServersMessage())
            return iceServersFuture
        }

    init {
        messageListeners = HashMap()
        connectionState = SimpleObjectProperty()
        sessionId = SimpleObjectProperty()
        // TODO note to myself; seriously, create a single gson instance (or builder) and put it all there
        gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(VictoryCondition::class.java, VictoryConditionTypeAdapter.INSTANCE)
                .registerTypeAdapter(GameStatus::class.java, GameStateTypeAdapter.INSTANCE)
                .registerTypeAdapter(GameAccess::class.java, GameAccessTypeAdapter.INSTANCE)
                .registerTypeAdapter(ClientMessageType::class.java, ClientMessageTypeTypeAdapter.INSTANCE)
                .registerTypeAdapter(FafServerMessageType::class.java, ServerMessageTypeTypeAdapter.INSTANCE)
                .registerTypeAdapter(GpgServerMessageType::class.java, GpgServerMessageTypeTypeAdapter.INSTANCE)
                .registerTypeAdapter(MessageTarget::class.java, MessageTargetTypeAdapter.INSTANCE)
                .registerTypeAdapter(ServerMessage::class.java, ServerMessageTypeAdapter.INSTANCE)
                .registerTypeAdapter(RatingRange::class.java, RatingRangeTypeAdapter.INSTANCE)
                .create()

        addOnMessageListener(NoticeMessage::class.java) { this.onNotice(it) }
        addOnMessageListener(SessionMessage::class.java) { this.onSessionInitiated(it) }
        addOnMessageListener(LoginMessage::class.java) { this.onFafLoginSucceeded(it) }
        addOnMessageListener(GameLaunchMessage::class.java) { this.onGameLaunchInfo(it) }
        addOnMessageListener(AuthenticationFailedMessage::class.java) { this.dispatchAuthenticationFailed(it) }
        addOnMessageListener(AvatarMessage::class.java) { this.onAvatarMessage(it) }
        addOnMessageListener(IceServersServerMessage::class.java) { this.onIceServersMessage(it) }
    }

    private fun onAvatarMessage(avatarMessage: AvatarMessage) {
        avatarsFuture!!.complete(avatarMessage.avatarList)
    }

    private fun onIceServersMessage(iceServersServerMessage: IceServersServerMessage) {
        iceServersFuture!!.complete(iceServersServerMessage.getIceServers())
    }

    private fun onNotice(noticeMessage: NoticeMessage) {
        if (noticeMessage.text == null) {
            return
        }
        notificationService.addNotification(ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.text, noticeMessage.severity,
                listOf<Action>(DismissAction(i18n))))
    }

    override fun <T : ServerMessage> addOnMessageListener(type: Class<T>, listener: Consumer<T>) {
        if (!messageListeners.containsKey(type)) {
            messageListeners[type] = LinkedList()
        }
        messageListeners[type].add(listener as Consumer<ServerMessage>)
    }

    override fun <T : ServerMessage> removeOnMessageListener(type: Class<T>, listener: Consumer<T>) {
        messageListeners[type].remove(listener)
    }

    override fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState> {
        return connectionState
    }

    override fun connectAndLogIn(username: String, password: String): CompletableFuture<LoginMessage> {
        sessionFuture = CompletableFuture()
        loginFuture = CompletableFuture()
        this.username = username
        this.password = password

        // TODO extract class?
        fafConnectionTask = object : Task<Void>() {

            @Throws(Exception::class)
            override fun call(): Void? {
                while (!isCancelled) {
                    val server = clientProperties.getServer()
                    val serverHost = server.getHost()
                    val serverPort = server.getPort()

                    logger.info("Trying to connect to FAF server at {}:{}", serverHost, serverPort)
                    Platform.runLater { connectionState.set(ConnectionState.CONNECTING) }


                    try {
                        Socket(serverHost, serverPort).use({ fafServerSocket ->
                            fafServerSocket.getOutputStream().use({ outputStream ->
                                this@FafServerAccessorImpl.fafServerSocket = fafServerSocket

                                fafServerSocket.setKeepAlive(true)

                                localIp = fafServerSocket.getLocalAddress().getHostAddress()

                                serverWriter = createServerWriter(outputStream)

                                writeToServer(InitSessionMessage(Version.VERSION))

                                logger.info("FAF server connection established")
                                Platform.runLater { connectionState.set(ConnectionState.CONNECTED) }

                                blockingReadServer(fafServerSocket)
                            })
                        })
                    } catch (e: IOException) {
                        Platform.runLater { connectionState.set(ConnectionState.DISCONNECTED) }
                        if (isCancelled) {
                            logger.debug("Connection to FAF server has been closed")
                        } else {
                            logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e)
                            Thread.sleep(RECONNECT_DELAY)
                        }
                    }

                }
                return null
            }

            override fun cancelled() {
                IOUtils.closeQuietly(serverWriter)
                IOUtils.closeQuietly(fafServerSocket)
                logger.debug("Closed connection to FAF lobby server")
            }
        }
        executeInBackground(fafConnectionTask)
        return loginFuture
    }

    override fun requestHostGame(newGameInfo: NewGameInfo): CompletableFuture<GameLaunchMessage> {
        val hostGameMessage = HostGameMessage(
                if (StringUtils.isEmpty(newGameInfo.getPassword())) GameAccess.PUBLIC else GameAccess.PASSWORD,
                newGameInfo.getMap(),
                newGameInfo.getTitle(),
                BooleanArray(0),
                newGameInfo.getFeaturedMod().getTechnicalName(),
                newGameInfo.getPassword(), null,
                newGameInfo.getGameVisibility()
        )

        gameLaunchFuture = CompletableFuture()
        writeToServer(hostGameMessage)
        return gameLaunchFuture
    }

    override fun requestJoinGame(gameId: Int, password: String): CompletableFuture<GameLaunchMessage> {
        val joinGameMessage = JoinGameMessage(gameId, password)

        gameLaunchFuture = CompletableFuture()
        writeToServer(joinGameMessage)
        return gameLaunchFuture
    }

    @PreDestroy
    override fun disconnect() {
        if (fafConnectionTask != null) {
            fafConnectionTask!!.cancel(true)
        }
    }

    override fun reconnect() {
        IOUtils.closeQuietly(fafServerSocket)
    }

    override fun addFriend(playerId: Int) {
        writeToServer(AddFriendMessage(playerId))
    }

    override fun addFoe(playerId: Int) {
        writeToServer(AddFoeMessage(playerId))
    }

    override fun startSearchLadder1v1(faction: Faction): CompletableFuture<GameLaunchMessage> {
        gameLaunchFuture = CompletableFuture()
        writeToServer(SearchLadder1v1ClientMessage(faction))
        return gameLaunchFuture
    }

    override fun stopSearchingRanked() {
        writeToServer(StopSearchLadder1v1ClientMessage())
        gameLaunchFuture = null
    }

    override fun sendGpgMessage(message: GpgGameMessage) {
        writeToServer(message)
    }

    override fun removeFriend(playerId: Int) {
        writeToServer(RemoveFriendMessage(playerId))
    }

    override fun removeFoe(playerId: Int) {
        writeToServer(RemoveFoeMessage(playerId))
    }

    override fun selectAvatar(url: URL) {
        writeToServer(SelectAvatarMessage(url))
    }

    override fun restoreGameSession(id: Int) {
        writeToServer(RestoreGameSessionMessage(id))
    }

    private fun createServerWriter(outputStream: OutputStream): ServerWriter {
        val serverWriter = ServerWriter(outputStream)
        serverWriter.registerMessageSerializer(ClientMessageSerializer(), ClientMessage::class.java)
        serverWriter.registerMessageSerializer(StringSerializer(), String::class.java)
        serverWriter.registerMessageSerializer(GpgClientMessageSerializer(), GpgGameMessage::class.java)
        return serverWriter
    }

    private fun writeToServer(message: SerializableMessage) {
        serverWriter!!.write(message)
    }

    public override fun onServerMessage(message: String?) {
        val serverCommand = ServerCommand.fromString(message)
        if (serverCommand != null) {
            dispatchServerMessage(serverCommand)
        } else {
            parseServerObject(message)
        }
    }

    private fun dispatchServerMessage(serverCommand: ServerCommand) {
        when (serverCommand) {
            ServerCommand.PING -> {
                logger.debug("Server PINGed")
                onServerPing()
            }

            ServerCommand.PONG -> logger.debug("Server PONGed")

            else -> logger.warn("Unknown server response: {}", serverCommand)
        }
    }

    private fun parseServerObject(jsonString: String?) {
        try {
            val serverMessage = gson.fromJson(jsonString, ServerMessage::class.java)
            if (serverMessage == null) {
                logger.debug("Discarding unimplemented server message: {}", jsonString)
                return
            }

            var messageClass: Class<*> = serverMessage.javaClass
            while (messageClass != Any::class.java) {
                (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(messageClass, emptyList())
                        .forEach { consumer -> consumer.accept(serverMessage) }
                messageClass = messageClass.superclass
            }
            for (type in ClassUtils.getAllInterfacesForClassAsSet(messageClass)) {
                (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(messageClass, emptyList())
                        .forEach { consumer -> consumer.accept(serverMessage) }
            }
        } catch (e: JsonSyntaxException) {
            logger.warn("Could not deserialize message: " + jsonString!!, e)
        }

    }

    private fun onServerPing() {
        writeToServer(PongMessage())
    }

    private fun dispatchAuthenticationFailed(message: AuthenticationFailedMessage) {
        fafConnectionTask!!.cancel()
        loginFuture!!.completeExceptionally(LoginFailedException(message.text))
        loginFuture = null
    }

    private fun onFafLoginSucceeded(loginServerMessage: LoginMessage) {
        logger.info("FAF login succeeded")

        if (loginFuture != null) {
            loginFuture!!.complete(loginServerMessage)
            loginFuture = null
        }
    }

    private fun onSessionInitiated(sessionMessage: SessionMessage) {
        logger.info("FAF session initiated, session ID: {}", sessionMessage.session)
        this.sessionId.set(sessionMessage.session)
        sessionFuture!!.complete(sessionMessage)
        logIn(username, password)
    }

    private fun logIn(username: String?, password: String?) {
        try {
            val uniqueId = uidService.generate(sessionId.get().toString(), preferencesService.fafDataDirectory.resolve("uid.log"))
            writeToServer(LoginClientMessage(username, Hashing.sha256().hashString(password!!, UTF_8).toString(), sessionId.get(), uniqueId, localIp))
        } catch (e: IOException) {
            onUIDNotExecuted(e)
        }

    }

    @VisibleForTesting
    fun onUIDNotExecuted(e: Exception) {
        logger.error("UID.exe not executed", e)
        if (e.message == null) {
            return
        }
        notificationService.addNotification(ImmediateNotification(i18n.get("UIDNotExecuted"), e.message, Severity.ERROR,
                listOf<Action>(ReportAction(i18n, reportingService, e))))
    }

    private fun onGameLaunchInfo(gameLaunchMessage: GameLaunchMessage) {
        gameLaunchFuture!!.complete(gameLaunchMessage)
        gameLaunchFuture = null
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    override fun ping() {
        if (fafServerSocket == null || !fafServerSocket!!.isConnected || serverWriter == null) {
            return
        }
        writeToServer(PingMessage.INSTANCE)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val RECONNECT_DELAY: Long = 3000
    }
}
