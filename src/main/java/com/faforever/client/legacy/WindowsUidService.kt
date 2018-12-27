package com.faforever.client.legacy

import com.faforever.client.os.OsUtils
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import java.io.IOException
import java.nio.file.Path


@Lazy
@Service
@Profile("windows")
class WindowsUidService : UidService {

    @Throws(IOException::class)
    override fun generate(sessionId: String, logFile: Path): String {
        val uidDir = System.getProperty("nativeDir", "lib")
        return OsUtils.execAndGetOutput(String.format("%s/faf-uid.exe %s", uidDir, sessionId))
    }
}
