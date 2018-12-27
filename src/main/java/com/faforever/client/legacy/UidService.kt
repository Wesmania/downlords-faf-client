package com.faforever.client.legacy

import java.io.IOException
import java.nio.file.Path

interface UidService {

    @Throws(IOException::class)
    fun generate(sessionId: String, logFile: Path): String
}
