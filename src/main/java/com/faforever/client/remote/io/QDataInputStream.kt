package com.faforever.client.remote.io

import java.io.Closeable
import java.io.DataInput
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class QDataInputStream @JvmOverloads constructor(private val dataInput: DataInput, private val charset: Charset = StandardCharsets.UTF_16BE) : InputStream() {

    @Throws(IOException::class)
    fun readQString(): String? {
        val stringSize = dataInput.readInt()
        if (stringSize == -1) {
            return null
        }

        val buffer = ByteArray(stringSize)
        dataInput.readFully(buffer)
        return String(buffer, charset)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return dataInput.readUnsignedByte()
    }

    @Throws(IOException::class)
    override fun close() {
        if (dataInput is Closeable) {
            (dataInput as Closeable).close()
        }
    }

    @Throws(IOException::class)
    fun readInt(): Int {
        return dataInput.readInt()
    }

    /**
     * Skip the "block size" bytes, since we just don't care.
     */
    @Throws(IOException::class)
    fun skipBlockSize() {
        dataInput.skipBytes(Integer.BYTES)
    }
}
