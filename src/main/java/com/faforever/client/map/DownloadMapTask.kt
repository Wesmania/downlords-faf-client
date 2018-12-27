package com.faforever.client.map

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.commons.io.Unzipper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedInputStream
import java.lang.invoke.MethodHandles
import java.net.URL
import java.net.URLConnection
import java.nio.file.Path
import java.util.Objects
import java.util.zip.ZipInputStream

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DownloadMapTask @Inject
constructor(private val preferencesService: PreferencesService, private val i18n: I18n) : CompletableTask<Void>(CompletableTask.Priority.HIGH) {

    private var mapUrl: URL? = null
    private var folderName: String? = null

    @Throws(Exception::class)
    public override fun call(): Void? {
        Objects.requireNonNull<URL>(mapUrl, "mapUrl has not been set")
        Objects.requireNonNull<String>(folderName, "folderName has not been set")

        updateTitle(i18n.get("mapDownloadTask.title", folderName))
        logger.info("Downloading map {} from {}", folderName, mapUrl)

        val urlConnection = mapUrl!!.openConnection()
        val bytesToRead = urlConnection.contentLength

        val targetDirectory = preferencesService.preferences!!.forgedAlliance.customMapsDirectory

        ZipInputStream(BufferedInputStream(urlConnection.getInputStream())).use { inputStream ->
            Unzipper.from(inputStream)
                    .to(targetDirectory)
                    .totalBytes(bytesToRead.toLong())
                    .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                    .unzip()
        }

        return null
    }

    fun setMapUrl(mapUrl: URL) {
        this.mapUrl = mapUrl
    }

    fun setFolderName(folderName: String) {
        this.folderName = folderName
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
