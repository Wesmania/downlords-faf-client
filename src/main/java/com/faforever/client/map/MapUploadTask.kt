package com.faforever.client.map

import com.faforever.client.api.FafApiAccessor
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
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

import javax.annotation.PostConstruct
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
class MapUploadTask @Inject
constructor(private val preferencesService: PreferencesService, private val fafApiAccessor: FafApiAccessor, private val i18n: I18n) : CompletableTask<Void>(CompletableTask.Priority.HIGH) {

    private var mapPath: Path? = null
    private var isRanked: Boolean? = null

    @PostConstruct
    internal fun postConstruct() {
        updateTitle(i18n.get("mapVault.upload.uploading"))
    }

    @Throws(Exception::class)
    override fun call(): Void? {
        Validator.notNull(mapPath, "mapPath must not be null")
        Validator.notNull(isRanked, "isRanked must not be null")

        ResourceLocks.acquireUploadLock()
        val cacheDirectory = preferencesService.cacheDirectory
        Files.createDirectories(cacheDirectory)
        val tmpFile = createTempFile(cacheDirectory, "map", ".zip")

        try {
            logger.debug("Zipping map {} to {}", mapPath, tmpFile)
            updateTitle(i18n.get("mapVault.upload.compressing"))

            val locale = i18n.userSpecificLocale
            val byteListener = { written, total ->
                updateMessage(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale)))
                updateProgress(written, total)
            }

            ZipOutputStream(BufferedOutputStream(newOutputStream(tmpFile))).use { zipOutputStream ->
                Zipper.of(mapPath)
                        .to(zipOutputStream)
                        .listener(byteListener)
                        .zip()
            }

            logger.debug("Uploading map {} as {}", mapPath, tmpFile)
            updateTitle(i18n.get("mapVault.upload.uploading"))

            fafApiAccessor.uploadMap(tmpFile, isRanked!!, byteListener)
            return null
        } finally {
            Files.delete(tmpFile)
            ResourceLocks.freeUploadLock()
        }
    }

    fun setMapPath(mapPath: Path) {
        this.mapPath = mapPath
    }

    fun setRanked(ranked: Boolean) {
        isRanked = ranked
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
