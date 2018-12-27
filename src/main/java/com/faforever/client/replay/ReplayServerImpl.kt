package com.faforever.client.replay

import com.faforever.client.config.ClientProperties
import com.faforever.client.game.Game
import com.faforever.client.game.GameService
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.Action
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.Severity
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.update.ClientUpdateService
import com.faforever.client.user.UserService
import com.google.common.primitives.Bytes
import lombok.extern.slf4j.Slf4j
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.CompletableFuture

import com.github.nocatch.NoCatch.noCatch

@Lazy
@Component
@Slf4j
class ReplayServerImpl @Inject
constructor(private val clientProperties: ClientProperties, private val notificationService: NotificationService, private val i18n: I18n,
            private val gameService: GameService, private val userService: UserService, private val replayFileWriter: ReplayFileWriter,
            private val clientUpdateService: ClientUpdateService) : ReplayServer {

    private var replayInfo: LocalReplayInfo? = null
    private var serverSocket: ServerSocket? = null
    private var stoppedGracefully: Boolean = false

    override fun stop() {
        if (serverSocket == null) {
            return
        }
        stoppedGracefully = true
        noCatch { serverSocket!!.close() }
    }

    override fun start(gameId: Int): CompletableFuture<Int> {
        stoppedGracefully = false
        val future = CompletableFuture<Int>()
        Thread {
            val remoteReplayServerHost = clientProperties.getReplay().getRemoteHost()
            val remoteReplayServerPort = clientProperties.getReplay().getRemotePort()

            log.debug("Connecting to replay server at '{}:{}'", remoteReplayServerHost, remoteReplayServerPort)

            try {
                ServerSocket(0).use { localSocket ->
                    log.debug("Opening local replay server on port {}", localSocket.localPort)
                    this.serverSocket = localSocket
                    future.complete(serverSocket!!.localPort)

                    try {
                        Socket(remoteReplayServerHost, remoteReplayServerPort!!).use({ remoteReplayServerSocket -> DataOutputStream(remoteReplayServerSocket.getOutputStream()).use { fafReplayOutputStream -> recordAndRelay(gameId, localSocket, fafReplayOutputStream) } })
                    } catch (e: ConnectException) {
                        log.warn("Could not connect to remote replay server", e)
                        notificationService.addNotification(PersistentNotification(i18n.get("replayServer.unreachable"), Severity.WARN))
                        recordAndRelay(gameId, localSocket, null)
                    }
                }
            } catch (e: IOException) {
                if (stoppedGracefully) {
                    return@new Thread(() -> {
                        String remoteReplayServerHost = clientProperties . getReplay ().getRemoteHost();
                        Integer remoteReplayServerPort = clientProperties . getReplay ().getRemotePort();

                        log.debug("Connecting to replay server at '{}:{}'", remoteReplayServerHost, remoteReplayServerPort);

                        try (ServerSocket localSocket = new ServerSocket(0)) {
                            log.debug("Opening local replay server on port {}", localSocket.getLocalPort());
                            this.serverSocket = localSocket;
                            future.complete(serverSocket.getLocalPort());

                            try Socket remoteReplayServerSocket = new Socket(remoteReplayServerHost, remoteReplayServerPort);
                                DataOutputStream fafReplayOutputStream = new DataOutputStream(remoteReplayServerSocket.getOutputStream())) {
                                    recordAndRelay(gameId, localSocket, fafReplayOutputStream);
                                } catch (ConnectException e) {
                                    log.warn("Could not connect to remote replay server", e);
                                    notificationService.addNotification(new PersistentNotification i18n.get("replayServer.unreachable"), Severity.WARN));
                                    recordAndRelay(gameId, localSocket, null);
                                }
                            } catch (IOException e) {
                            if (stoppedGracefully) {
                                return;
                            }
                            future.completeExceptionally(e);
                            log.warn("Error in replay server", e);
                            notificationService.addNotification(new PersistentNotification i18n.get("replayServer.listeningFailed"),
                                    Severity.WARN, Collections.singletonList(new Action i18n.get("replayServer.retry"), event -> start(gameId)))
                            ));
                        }
                        }).start
                }
                future.completeExceptionally(e)
                log.warn("Error in replay server", e)
                notificationService.addNotification(PersistentNotification(
                        i18n.get("replayServer.listeningFailed"),
                        Severity.WARN, listOf(Action(i18n.get("replayServer.retry")) { event -> start(gameId) })
                ))
            }
        }.start()
        return future
    }

    private fun initReplayInfo(uid: Int) {
        replayInfo = LocalReplayInfo()
        replayInfo!!.uid = uid
        replayInfo!!.launchedAt = pythonTime()
        replayInfo!!.versionInfo = HashMap()
        replayInfo!!.versionInfo!!["lobby"] = String.format("dfaf-%s", clientUpdateService.currentVersion.canonical)
    }

    /**
     * @param fafReplayOutputStream if `null`, the replay won't be relayed
     */
    @Throws(IOException::class)
    private fun recordAndRelay(uid: Int, serverSocket: ServerSocket, fafReplayOutputStream: OutputStream?) {
        val socket = serverSocket.accept()
        log.debug("Accepted connection from {}", socket.remoteSocketAddress)

        initReplayInfo(uid)

        val replayData = ByteArrayOutputStream()

        var connectionToServerLost = false
        val buffer = ByteArray(REPLAY_BUFFER_SIZE)
        try {
            socket.getInputStream().use { inputStream ->
                var bytesRead: Int
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (replayData.size() == 0 && Bytes.indexOf(buffer, LIVE_REPLAY_PREFIX) != -1) {
                        val dataBeginIndex = Bytes.indexOf(buffer, 0x00.toByte()) + 1
                        replayData.write(buffer, dataBeginIndex, bytesRead - dataBeginIndex)
                    } else {
                        replayData.write(buffer, 0, bytesRead)
                    }

                    if (!connectionToServerLost && fafReplayOutputStream != null) {
                        try {
                            fafReplayOutputStream.write(buffer, 0, bytesRead)
                        } catch (e: SocketException) {
                            // In case we lose connection to the replay server, just stop writing to it
                            log.warn("Connection to replay server lost ({})", e.message)
                            connectionToServerLost = true
                        }

                    }
                }
            }
        } catch (e: Exception) {
            log.warn("Error while recording replay", e)
            throw e
        }

        log.debug("FAF has disconnected, writing replay data to file")
        finishReplayInfo()
        replayFileWriter.writeReplayDataToFile(replayData, replayInfo)
    }

    private fun finishReplayInfo() {
        val game = gameService.getByUid(replayInfo!!.uid!!)

        replayInfo!!.updateFromGameInfoBean(game)
        replayInfo!!.gameEnd = pythonTime()
        replayInfo!!.recorder = userService.username
        replayInfo!!.state = GameStatus.CLOSED
        replayInfo!!.isComplete = true
    }

    companion object {

        /**
         * Size for buffer used to send data to the live replay server. The buffer needs to be large enough to not flush too
         * many times (after all, it's a TCP stream so we don't want to send single bytes) but small enough to not delay data
         * for too long. I don't have any data right now but it can be expected that the replay stream produces about 70 bytes
         * per second (See #973).
         */
        private val REPLAY_BUFFER_SIZE = 128

        /**
         * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
         */
        private val LIVE_REPLAY_PREFIX = byteArrayOf('P'.toByte(), '/'.toByte())

        /**
         * Returns the current millis the same way as python does since this is what's stored in the replay files *yay*.
         */
        private fun pythonTime(): Double {
            return (System.currentTimeMillis() / 1000).toDouble()
        }
    }
}
