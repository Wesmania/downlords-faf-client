package com.faforever.client.fa.relay.ice

import com.faforever.client.fa.relay.ConnectToPeerMessage
import com.faforever.client.fa.relay.DisconnectFromPeerMessage
import com.faforever.client.fa.relay.GpgClientCommand
import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.fa.relay.HostGameMessage
import com.faforever.client.fa.relay.JoinGameMessage
import com.faforever.client.fa.relay.LobbyMode
import com.faforever.client.fa.relay.event.GameFullEvent
import com.faforever.client.fa.relay.event.RehostRequestEvent
import com.faforever.client.fa.relay.ice.event.GpgGameMessageEvent
import com.faforever.client.fa.relay.ice.event.IceAdapterStateChanged
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.remote.FafService
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.IceServerMessage
import com.faforever.client.remote.domain.IceServersServerMessage
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.nbarraille.jjsonrpc.JJsonPeer
import com.nbarraille.jjsonrpc.TcpClient
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.bridj.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.util.SocketUtils

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Inject
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.ConnectException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.HashMap
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import com.faforever.client.os.OsUtils.gobbleLines
import java.util.Arrays.asList

@Component
@Lazy
@Slf4j
class IceAdapterImpl @Inject
constructor(private val applicationContext: ApplicationContext, private val playerService: PlayerService,
            private val eventBus: EventBus, private val fafService: FafService) : IceAdapter {
    private val iceAdapterProxy: IceAdapterApi

    private var iceAdapterClientFuture: CompletableFuture<Int>? = null
    private var process: Process? = null
    private var lobbyInitMode: LobbyMode? = null
    private var peer: JJsonPeer? = null

    init {

        iceAdapterProxy = newIceAdapterProxy()
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
        fafService.addOnMessageListener(JoinGameMessage::class.java) { message -> iceAdapterProxy.joinGame(message.username, message.peerUid) }
        fafService.addOnMessageListener(HostGameMessage::class.java) { message -> iceAdapterProxy.hostGame(message.map) }
        fafService.addOnMessageListener(ConnectToPeerMessage::class.java) { message -> iceAdapterProxy.connectToPeer(message.username, message.peerUid, message.isOffer) }
        fafService.addOnMessageListener(GameLaunchMessage::class.java, Consumer<GameLaunchMessage> { this.updateLobbyModeFromGameInfo(it) })
        fafService.addOnMessageListener(DisconnectFromPeerMessage::class.java) { message -> iceAdapterProxy.disconnectFromPeer(message.uid) }
        fafService.addOnMessageListener(IceServerMessage::class.java) { message -> iceAdapterProxy.iceMsg(message.sender, message.record) }
    }

    @SneakyThrows
    private fun toIceServers(iceServers: List<IceServersServerMessage.IceServer>): List<Map<String, String>> {
        return iceServers.stream()
                .map<Map<String, String>>(Function<IceServer, Map<String, String>> { this.toIceServer(it) })
                .collect<List<Map<String, String>>, Any>(Collectors.toList())
    }

    private fun toIceServer(iceServer: IceServersServerMessage.IceServer): Map<String, String> {
        val map = HashMap<String, String>()
        map["url"] = iceServer.getUrl()

        if (iceServer.getCredential() != null) {
            map["credential"] = iceServer.getCredential()
            map["credentialType"] = iceServer.getCredentialType()
        }
        if (iceServer.getUsername() != null) {
            map["username"] = iceServer.getUsername()
        }
        return map
    }

    @Subscribe
    fun onIceAdapterStateChanged(event: IceAdapterStateChanged) {
        when (event.newState) {
            "Disconnected" -> iceAdapterProxy.quit()
        }
    }

    @Subscribe
    fun onGpgGameMessage(event: GpgGameMessageEvent) {
        val gpgGameMessage = event.gpgGameMessage
        val command = gpgGameMessage.command

        if (command == GpgClientCommand.REHOST) {
            eventBus.post(RehostRequestEvent())
            return
        }
        if (command == GpgClientCommand.GAME_FULL) {
            eventBus.post(GameFullEvent())
            return
        }

        fafService.sendGpgGameMessage(gpgGameMessage)
    }

    override fun start(): CompletableFuture<Int> {
        iceAdapterClientFuture = CompletableFuture()
        val thread = Thread {
            val nativeDir = System.getProperty("nativeDir", "lib")

            val adapterPort = SocketUtils.findAvailableTcpPort()
            val gpgPort = SocketUtils.findAvailableTcpPort()

            val currentPlayer = playerService.currentPlayer
                    .orElseThrow { IllegalStateException("Player has not been set") }


            val workDirectory = Paths.get(nativeDir).toAbsolutePath()
            val cmd = arrayOf(getBinaryName(workDirectory), "--id", currentPlayer.id.toString(), "--login", currentPlayer.username, "--rpc-port", adapterPort.toString(), "--gpgnet-port", gpgPort.toString())

            try {
                val processBuilder = ProcessBuilder()
                processBuilder.directory(workDirectory.toFile())
                processBuilder.command(*cmd)

                log.debug("Starting ICE adapter with command: {}", asList<T>(*cmd))
                process = processBuilder.start()
                val logger = LoggerFactory.getLogger("faf-ice-adapter")
                gobbleLines(process!!.inputStream, Consumer<String> { logger.debug(it) })
                gobbleLines(process!!.errorStream, Consumer<String> { logger.error(it) })

                val iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks::class.java)

                for (attempt in 0 until CONNECTION_ATTEMPTS) {
                    try {
                        val tcpClient = TcpClient("localhost", adapterPort, iceAdapterCallbacks)
                        peer = tcpClient.peer

                        setIceServers()
                        setLobbyInitMode()
                        break
                    } catch (e: ConnectException) {
                        logger.debug("Could not connect to ICE adapter (attempt {}/{})", attempt, CONNECTION_ATTEMPTS)
                    }

                }

                iceAdapterClientFuture!!.complete(gpgPort)

                val exitCode = process!!.waitFor()
                if (exitCode == 0) {
                    logger.debug("ICE adapter terminated normally")
                } else {
                    logger.warn("ICE adapter terminated with exit code: {}", exitCode)
                }
            } catch (e: Exception) {
                iceAdapterClientFuture!!.completeExceptionally(e)
            }
        }
        thread.isDaemon = true
        thread.start()

        return iceAdapterClientFuture
    }

    private fun getBinaryName(workDirectory: Path): String {
        return if (Platform.isWindows()) workDirectory.resolve("faf-ice-adapter.exe").toString() else "./faf-ice-adapter"
    }

    private fun setIceServers() {
        fafService.iceServers
                .thenAccept { iceServers -> iceAdapterProxy.setIceServers(toIceServers(iceServers)) }
                .exceptionally { throwable ->
                    log.warn("Could not get ICE servers", throwable)
                    null
                }
    }

    private fun setLobbyInitMode() {
        when (lobbyInitMode) {
            LobbyMode.DEFAULT_LOBBY -> iceAdapterProxy.setLobbyInitMode("normal")
            LobbyMode.NO_LOBBY -> iceAdapterProxy.setLobbyInitMode("auto")
        }
    }

    private fun newIceAdapterProxy(): IceAdapterApi {
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf<Class<*>>(IceAdapterApi::class.java)
        ) { proxy: Any, method: Method, args: Array<Any> ->
            if ("toString" == method.name) {
                return@Proxy.newProxyInstance "ICE adapter proxy"
            }

            val argList = if (args == null) emptyList() else asList(*args)
            if (peer == null || !peer!!.isAlive && "quit" != method.name) {
                log.warn("Ignoring call to ICE adapter as we are not connected: {}({})", method.name, argList)
                return@Proxy.newProxyInstance null
            }
            log.debug("Calling {}({})", method.name, argList)
            if (method.returnType == Void.TYPE) {
                peer!!.sendAsyncRequest(method.name, argList, null, true)
                return@Proxy.newProxyInstance null
            } else {
                return@Proxy.newProxyInstance peer !!. sendSyncRequest method.name, argList, true)
            }
        } as IceAdapterApi
    }

    private fun updateLobbyModeFromGameInfo(gameLaunchMessage: GameLaunchMessage) {
        if (KnownFeaturedMod.LADDER_1V1.technicalName == gameLaunchMessage.mod) {
            lobbyInitMode = LobbyMode.DEFAULT_LOBBY
        } else {
            lobbyInitMode = LobbyMode.DEFAULT_LOBBY
        }
    }

    @PreDestroy
    override fun stop() {
        Optional.ofNullable(iceAdapterProxy).ifPresent(Consumer<IceAdapterApi> { it.quit() })
        peer = null
    }

    companion object {

        private val CONNECTION_ATTEMPTS = 5
    }
}
