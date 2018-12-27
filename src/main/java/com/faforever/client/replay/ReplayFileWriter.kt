package com.faforever.client.replay

import java.io.ByteArrayOutputStream
import java.io.IOException

interface ReplayFileWriter {

    @Throws(IOException::class)
    fun writeReplayDataToFile(replayData: ByteArrayOutputStream, replayInfo: LocalReplayInfo)
}
