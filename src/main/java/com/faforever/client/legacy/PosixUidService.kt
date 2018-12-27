package com.faforever.client.legacy

import com.faforever.client.os.OsUtils
import com.google.common.collect.Sets
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission


@Lazy
@Service
@Profile("linux", "mac")
class PosixUidService : UidService {

    @Throws(IOException::class)
    override fun generate(sessionId: String, logFile: Path): String {
        val uidDir = System.getProperty("nativeDir", "lib")
        val uidPath = Paths.get(uidDir).resolve("faf-uid")
        Files.setPosixFilePermissions(uidPath, Sets.immutableEnumSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE))
        return OsUtils.execAndGetOutput(String.format("%s %s", uidPath.toAbsolutePath(), sessionId))
    }
}
