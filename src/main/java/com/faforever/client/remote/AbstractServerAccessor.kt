package com.faforever.client.remote

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.remote.domain.FafServerMessage
import com.faforever.client.remote.io.QDataInputStream
import org.apache.commons.compress.utils.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.PreDestroy
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.Socket

/**
 * Super class for all server accessors.
 */
abstract class AbstractServerAccessor {

    private var stopped: Boolean = false
    private var dataInput: QDataInputStream? = null

    /**
     * Reads data received from the server and dispatches it. So far, there are two types of data sent by the server:
     *  1. **Server messages** are simple words like ACK or PING, followed by some bytes..
     *  1. **Objects** are JSON-encoded objects like preferences or player information. Those are converted into a
     * [FafServerMessage]  I'm not yet happy with those terms, so any suggestions are welcome.
     */
    @Throws(IOException::class)
    protected fun blockingReadServer(socket: Socket) {
        JavaFxUtil.assertBackgroundThread()

        dataInput = QDataInputStream(DataInputStream(BufferedInputStream(socket.getInputStream())))
        while (!stopped && !socket.isInputShutdown) {
            dataInput!!.skipBlockSize()
            val message = dataInput!!.readQString()

            logger.debug("Message from server: {}", message)

            try {
                onServerMessage(message)
            } catch (e: Exception) {
                logger.warn("Error while handling server message: " + message!!, e)
            }

        }

        logger.info("Connection to server {} has been closed", socket.remoteSocketAddress)
    }

    @Throws(IOException::class)
    protected abstract fun onServerMessage(message: String?)

    @PreDestroy
    @Throws(IOException::class)
    internal fun close() {
        stopped = true
        IOUtils.closeQuietly(dataInput)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

}
