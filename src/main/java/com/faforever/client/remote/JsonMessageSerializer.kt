package com.faforever.client.remote

import com.faforever.client.remote.domain.SerializableMessage
import com.faforever.client.remote.io.QDataWriter
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.serializer.Serializer

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.StringWriter
import java.io.Writer
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.util.Arrays

open class JsonMessageSerializer<T : SerializableMessage> : Serializer<T> {

    private var gson: Gson? = null

    // TODO Clean this up, such that the message is logged within ServerWriter and everything makes much more sense
    @Throws(IOException::class)
    override fun serialize(message: SerializableMessage, outputStream: OutputStream) {
        val byteArrayOutputStream = ByteArrayOutputStream()

        val jsonStringWriter = StringWriter()

        // Serialize the object into a StringWriter which is later send as one string block with its size prepended.
        getGson().toJson(message, message.javaClass, fixedJsonWriter(jsonStringWriter))

        val qDataWriter = QDataWriter(byteArrayOutputStream)
        qDataWriter.append(jsonStringWriter.toString())

        val byteArray = byteArrayOutputStream.toByteArray()

        if (logger.isDebugEnabled) {
            // Remove the first 4 bytes which contain the length of the following data
            var data = String(Arrays.copyOfRange(byteArray, 4, byteArray.size), StandardCharsets.UTF_16BE)

            for (stringToMask in message.stringsToMask) {
                data = data.replace("\"" + stringToMask + "\"", "\"" + CONFIDENTIAL_INFORMATION_MASK + "\"")
            }

            logger.debug("Writing to server: {}", data)
        }

        outputStream.write(byteArray)
    }

    private fun getGson(): Gson {
        if (gson == null) {
            val gsonBuilder = GsonBuilder()
                    .disableHtmlEscaping()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)

            addTypeAdapters(gsonBuilder)

            gson = gsonBuilder.create()
        }
        return gson
    }

    private fun fixedJsonWriter(writer: Writer): JsonWriter {
        // Does GSON suck because its separator can't be set, or python because it can't handle JSON without a space after colon?
        try {
            val jsonWriter = JsonWriter(writer)
            jsonWriter.serializeNulls = false

            val separatorField = JsonWriter::class.java.getDeclaredField("separator")
            separatorField.isAccessible = true
            separatorField.set(jsonWriter, ": ")

            return jsonWriter
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Allows subclasses to register additional type adapters. Super doesn't need to be called.
     */
    protected open fun addTypeAdapters(gsonBuilder: GsonBuilder) {
        // To be overridden by subclasses, if desired
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val CONFIDENTIAL_INFORMATION_MASK = "********"
    }
}
