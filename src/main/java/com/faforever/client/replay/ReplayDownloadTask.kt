package com.faforever.client.replay

import com.faforever.client.config.ClientProperties
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.commons.io.ByteCopier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReplayDownloadTask @Inject
constructor(private val i18n: I18n, private val clientProperties: ClientProperties, private val preferencesService: PreferencesService) : CompletableTask<Path>(CompletableTask.Priority.HIGH) {

    private var replayId: Int = 0

    @Throws(Exception::class)
    override fun call(): Path {
        updateTitle(i18n.get("mapReplayTask.title", replayId))

        val replayUrl = Replay.getReplayUrl(replayId, clientProperties.getVault().getReplayDownloadUrlFormat())

        logger.info("Downloading replay {} from {}", replayId, replayUrl)

        val urlConnection = URL(replayUrl).openConnection() as HttpURLConnection
        urlConnection.instanceFollowRedirects = true
        val bytesToRead = urlConnection.contentLength

        val tempSupComReplayFile = preferencesService.cacheDirectory.resolve(TEMP_FAF_REPLAY_FILE_NAME)

        Files.createDirectories(tempSupComReplayFile.parent)

        BufferedInputStream(urlConnection.inputStream).use { inputStream ->
            BufferedOutputStream(Files.newOutputStream(tempSupComReplayFile)).use { outputStream ->

                ByteCopier.from(inputStream)
                        .to(outputStream)
                        .totalBytes(bytesToRead.toLong())
                        .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                        .copy()

                return tempSupComReplayFile
            }
        }
    }


    fun setReplayId(replayId: Int) {
        this.replayId = replayId
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val TEMP_FAF_REPLAY_FILE_NAME = "temp.fafreplay"
    }
}
