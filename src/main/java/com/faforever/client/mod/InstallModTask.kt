package com.faforever.client.mod

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import com.faforever.commons.io.ByteCopier
import com.faforever.commons.io.Unzipper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects
import java.util.zip.ZipInputStream

import com.faforever.client.task.CompletableTask.Priority.HIGH

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
open class InstallModTask @Inject
constructor(private val preferencesService: PreferencesService, private val i18n: I18n) : CompletableTask<Void>(HIGH) {

    private var url: URL? = null

    @Throws(Exception::class)
    public override fun call(): Void? {
        Objects.requireNonNull<URL>(url, "url has not been set")

        val tempFile = Files.createTempFile(preferencesService.cacheDirectory, "mod", null)

        logger.info("Downloading mod {} to {}", url, tempFile)
        updateTitle(i18n.get("downloadingModTask.downloading", url))

        Files.createDirectories(tempFile.parent)

        val urlConnection = url!!.openConnection()
        val contentLength = urlConnection.contentLength

        ResourceLocks.acquireDownloadLock()
        try {
            urlConnection.getInputStream().use { inputStream ->
                Files.newOutputStream(tempFile).use { outputStream ->

                    ByteCopier.from(inputStream)
                            .to(outputStream)
                            .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                            .totalBytes(contentLength.toLong())
                            .copy()

                    extractMod(tempFile)
                }
            }
        } finally {
            ResourceLocks.freeDownloadLock()
            try {
                Files.deleteIfExists(tempFile)
            } catch (e: IOException) {
                logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e)
            }

        }
        return null
    }

    @Throws(IOException::class)
    private fun extractMod(tempFile: Path) {
        val modsDirectory = preferencesService.preferences!!.forgedAlliance.modsDirectory

        updateTitle(i18n.get("downloadingModTask.unzipping", modsDirectory))
        logger.info("Unzipping {} to {}", tempFile, modsDirectory)

        try {
            ZipInputStream(Files.newInputStream(tempFile)).use { zipInputStream ->
                ResourceLocks.acquireDiskLock()

                Unzipper.from(zipInputStream)
                        .to(modsDirectory)
                        .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                        .totalBytes(Files.size(tempFile))
                        .unzip()

            }
        } finally {
            ResourceLocks.freeDiskLock()
        }
    }

    fun setUrl(url: URL) {
        this.url = url
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
