package com.faforever.client.replay

import com.faforever.client.config.ClientProperties
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.commons.io.Bytes
import com.faforever.commons.replay.QtCompress
import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.StandardOpenOption.CREATE_NEW

@Lazy
@Component
class ReplayFileWriterImpl @Inject
constructor(private val i18n: I18n, private val clientProperties: ClientProperties, private val preferencesService: PreferencesService) : ReplayFileWriter {
    private val gson: Gson

    init {

        gson = ReplayFiles.gson()
    }

    @Throws(IOException::class)
    override fun writeReplayDataToFile(replayData: ByteArrayOutputStream, replayInfo: LocalReplayInfo) {
        val fileName = String.format(clientProperties.getReplay().getReplayFileFormat(), replayInfo.uid, replayInfo.recorder)
        val replayFile = preferencesService.replaysDirectory.resolve(fileName)

        logger.info("Writing replay file to {} ({})", replayFile, Bytes.formatSize(replayData.size().toLong(), i18n.userSpecificLocale))

        Files.createDirectories(replayFile.parent)

        Files.newBufferedWriter(replayFile, UTF_8, CREATE_NEW).use { writer ->
            val compressedBytes = QtCompress.qCompress(replayData.toByteArray())
            val base64ReplayData = BaseEncoding.base64().encode(compressedBytes)

            gson.toJson(replayInfo, writer)
            writer.write('\n')
            writer.write(base64ReplayData)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
