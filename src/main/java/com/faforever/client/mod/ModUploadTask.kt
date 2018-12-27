package com.faforever.client.mod

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import com.faforever.client.util.Validator
import com.faforever.commons.io.ByteCountListener
import com.faforever.commons.io.Zipper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedOutputStream
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.zip.ZipOutputStream

import com.faforever.commons.io.Bytes.formatSize
import java.nio.file.Files.createTempFile
import java.nio.file.Files.newOutputStream

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
open class ModUploadTask @Inject
constructor(private val preferencesService: PreferencesService, private val fafService: FafService, private val i18n: I18n) : CompletableTask<Void>(CompletableTask.Priority.HIGH) {

    private var modPath: Path? = null

    @Throws(Exception::class)
    public override fun call(): Void? {
        Validator.notNull(modPath, "modPath must not be null")

        ResourceLocks.acquireUploadLock()
        val cacheDirectory = preferencesService.cacheDirectory
        Files.createDirectories(cacheDirectory)
        val tmpFile = createTempFile(cacheDirectory, "mod", ".zip")

        try {
            logger.debug("Zipping mod {} to {}", modPath, tmpFile)
            updateTitle(i18n.get("modVault.upload.compressing"))

            val locale = i18n.userSpecificLocale
            val byteListener = { written, total ->
                updateMessage(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale)))
                updateProgress(written, total)
            }

            ZipOutputStream(BufferedOutputStream(newOutputStream(tmpFile))).use { zipOutputStream ->
                Zipper.of(modPath)
                        .to(zipOutputStream)
                        .listener(byteListener)
                        .zip()
            }

            logger.debug("Uploading mod {} as {}", modPath, tmpFile)
            updateTitle(i18n.get("modVault.upload.uploading"))

            fafService.uploadMod(tmpFile, byteListener)
            return null
        } finally {
            Files.delete(tmpFile)
            ResourceLocks.freeUploadLock()
        }
    }

    fun setModPath(modPath: Path) {
        this.modPath = modPath
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
