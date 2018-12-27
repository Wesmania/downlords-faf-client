package com.faforever.client.remote

import org.springframework.core.serializer.Serializer

import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class StringSerializer : Serializer<String> {

    @Throws(IOException::class)
    override fun serialize(string: String, outputStream: OutputStream) {
        outputStream.write(string.toByteArray(StandardCharsets.UTF_16))
    }
}
