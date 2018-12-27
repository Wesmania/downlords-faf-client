package com.faforever.client.replay


import com.faforever.commons.replay.ReplayData

import java.nio.file.Path

interface ReplayFileReader {

    /**
     * Returns the meta information about this replay (its FAF header)
     */
    fun parseMetaData(replayFile: Path): LocalReplayInfo

    /**
     * Returns the binary replay data.
     */
    fun readRawReplayData(replayFile: Path): ByteArray

    /**
     * Parses the actual replay data of the specified file and returns information such as chat messages, game options,
     * executed commands and so on.
     */
    fun parseReplay(path: Path): ReplayData
}
