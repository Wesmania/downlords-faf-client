package com.faforever.client.replay

import com.faforever.commons.replay.QtCompress
import com.faforever.commons.replay.ReplayData
import com.faforever.commons.replay.ReplayDataParser
import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path

@Lazy
@Component
@Slf4j
class ReplayFileReaderImpl : ReplayFileReader {

    private val gson: Gson

    init {
        gson = ReplayFiles.gson()
    }

    @SneakyThrows
    override fun parseMetaData(replayFile: Path): LocalReplayInfo {
        logger.debug("Parsing metadata of replay file: {}", replayFile)
        val lines = Files.readAllLines(replayFile)
        return gson.fromJson(lines[0], LocalReplayInfo::class.java)
    }

    @SneakyThrows
    override fun readRawReplayData(replayFile: Path): ByteArray {
        logger.debug("Reading replay file: {}", replayFile)
        val lines = Files.readAllLines(replayFile)
        return QtCompress.qUncompress(BaseEncoding.base64().decode(lines[1]))
    }

    override fun parseReplay(path: Path): ReplayData {
        return ReplayDataParser(path).parse()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
