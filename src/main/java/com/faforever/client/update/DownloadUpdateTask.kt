package com.faforever.client.update

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import com.faforever.commons.io.ByteCopier
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DownloadUpdateTask @Inject
constructor(private val i18n: I18n, private val preferencesService: PreferencesService) : CompletableTask<Path>(CompletableTask.Priority.MEDIUM) {

    private var updateInfo: UpdateInfo? = null

    @Throws(Exception::class)
    override fun call(): Path {
        updateTitle(i18n.get("clientUpdateDownloadTask.title"))
        val url = updateInfo!!.getUrl()

        val updateDirectory = preferencesService.cacheDirectory.resolve("update")
        val targetFile = updateDirectory.resolve(updateInfo!!.getFileName())
        Files.createDirectories(targetFile.getParent())

        val tempFile = Files.createTempFile(targetFile.getParent(), "update", null)

        ResourceLocks.acquireDownloadLock()
        try {
            url.openStream().use({ inputStream ->
                Files.newOutputStream(tempFile).use { outputStream ->
                    ByteCopier.from(inputStream)
                            .to(outputStream)
                            .totalBytes(updateInfo!!.getSize())
                            .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                            .copy()

                    Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
                }
            })
        } finally {
            ResourceLocks.freeDownloadLock()
            try {
                Files.deleteIfExists(tempFile)
            } catch (e: IOException) {
                logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e)
            }

        }

        return targetFile
    }

    fun setUpdateInfo(updateInfo: UpdateInfo) {
        this.updateInfo = updateInfo
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
