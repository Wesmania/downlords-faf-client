package com.faforever.client.remote.io

import java.io.IOException
import java.io.OutputStream
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class QDataWriter(private val out: OutputStream) : Writer() {

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        out.write(String(cbuf).substring(off, off + len).toByteArray(CHARSET))
    }

    @Throws(IOException::class)
    override fun append(csq: CharSequence?): Writer {
        if (csq == null) {
            writeInt32(-1)
            return this
        }

        val bytes = csq.toString().toByteArray(CHARSET)
        return appendWithSize(bytes)
    }

    @Throws(IOException::class)
    override fun flush() {
        out.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        out.close()
    }

    @Throws(IOException::class)
    fun writeInt32(v: Int) {
        out.write(v.ushr(24) and 0xFF)
        out.write(v.ushr(16) and 0xFF)
        out.write(v.ushr(8) and 0xFF)
        out.write(v and 0xFF)
    }

    /**
     * Appends the size of the given byte array to the stream followed by the byte array itself.
     */
    @Throws(IOException::class)
    fun appendWithSize(bytes: ByteArray): QDataWriter {
        writeInt32(bytes.size)
        out.write(bytes)
        return this
    }

    companion object {

        val CHARSET = StandardCharsets.UTF_16BE
    }
}
