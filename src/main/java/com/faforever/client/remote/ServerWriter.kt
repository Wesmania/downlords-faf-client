package com.faforever.client.remote

import com.faforever.client.remote.domain.SerializableMessage
import com.faforever.client.remote.io.QDataWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.serializer.Serializer

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.net.SocketException
import java.util.HashMap

/**
 * Sends data to the server. Classes should not use the server writer directly, but e.g. [com.faforever.client.remote.FafService] or
 * any other server accessor instead.
 */
class ServerWriter(outputStream: OutputStream) : Closeable {

    private val qDataWriter: QDataWriter
    private val objectWriters: MutableMap<Class<*>, Serializer<*>>

    init {
        qDataWriter = QDataWriter(DataOutputStream(BufferedOutputStream(outputStream)))
        objectWriters = HashMap()
    }

    fun registerMessageSerializer(objectSerializer: Serializer<*>, writableClass: Class<*>) {
        objectWriters[writableClass] = objectSerializer
    }

    fun write(`object`: SerializableMessage) {
        val clazz = `object`.javaClass

        val serializer = findSerializerForClass(clazz) as Serializer<SerializableMessage>
                ?: throw IllegalStateException("No object writer registered for type: $clazz")

        try {
            val outputStream = ByteArrayOutputStream()

            serializer.serialize(`object`, outputStream)

            synchronized(qDataWriter) {
                qDataWriter.appendWithSize(outputStream.toByteArray())
                qDataWriter.flush()
            }
        } catch (e: EOFException) {
            logger.debug("Server writer has been closed")
        } catch (e: SocketException) {
            logger.debug("Server writer has been closed")
        } catch (e: IOException) {
            logger.debug("Server writer has been closed", e)
        }

    }

    /**
     * Finds the appropriate serializer by walking up the type hierarchy. Interfaces are not checked.
     *
     * @return the appropriate serializer, or `null` if none was found
     */
    private fun findSerializerForClass(clazz: Class<*>): Serializer<*> {
        var classToCheck = clazz

        while (!objectWriters.containsKey(classToCheck) && classToCheck != Any::class.java) {
            classToCheck = classToCheck.superclass
        }

        return objectWriters[classToCheck]
    }

    @Throws(IOException::class)
    override fun close() {
        qDataWriter.close()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
