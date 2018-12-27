package com.faforever.client.io

import com.faforever.client.task.ResourceLocks
import com.faforever.commons.io.ByteCopier
import com.faforever.commons.io.ByteCountListener
import lombok.extern.slf4j.Slf4j
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
@Lazy
@Slf4j
class DownloadService {

    @Throws(IOException::class)
    fun downloadFile(url: URL, targetFile: Path, progressListener: ByteCountListener) {
        val tempFile = Files.createTempFile(targetFile.parent, "download", null)

        val urlConnection = url.openConnection() as HttpURLConnection

        ResourceLocks.acquireDownloadLock()
        try {
            url.openStream().use { inputStream ->
                Files.newOutputStream(tempFile).use { outputStream ->

                    ByteCopier.from(inputStream)
                            .to(outputStream)
                            .totalBytes(urlConnection.contentLength.toLong())
                            .listener(progressListener)
                            .copy()

                    Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } finally {
            ResourceLocks.freeDownloadLock()
            try {
                Files.deleteIfExists(tempFile)
            } catch (e: IOException) {
                log.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e)
            }

        }
    }
}
