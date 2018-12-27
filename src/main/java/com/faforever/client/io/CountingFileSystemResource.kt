package com.faforever.client.io

import com.faforever.commons.io.ByteCountListener
import lombok.SneakyThrows
import org.springframework.core.io.FileSystemResource

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.Optional

/**
 * Concrete implementation of [FileSystemResource] that counts the number of written bytes.
 */
class CountingFileSystemResource(file: Path, listener: ByteCountListener) : FileSystemResource(file.toFile()) {

    private val listener: ByteCountListener

    init {
        this.listener = Optional.ofNullable(listener)
                .orElseThrow { IllegalArgumentException("'listener' must not be null") }
    }

    @SneakyThrows
    override fun getInputStream(): InputStream {
        return CountingInputStream(file, listener)
    }

    private inner class CountingInputStream @Throws(FileNotFoundException::class)
    internal constructor(file: File, private val listener: ByteCountListener) : FileInputStream(file) {
        private val totalBytes: Long
        private var bytesDone: Long = 0

        init {
            this.totalBytes = file.length()
        }

        @Throws(IOException::class)
        override fun read(buffer: ByteArray): Int {
            val bytesRead = super.read(buffer)
            if (bytesRead != -1) {
                this.bytesDone += bytesRead.toLong()
            }

            this.listener.updateBytesWritten(this.bytesDone, totalBytes)
            return bytesRead
        }
    }
}
