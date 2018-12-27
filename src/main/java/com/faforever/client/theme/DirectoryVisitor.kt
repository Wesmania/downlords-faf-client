package com.faforever.client.theme

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.util.function.Consumer

class DirectoryVisitor(private val onDirectoryFoundListener: Consumer<Path>) : SimpleFileVisitor<Path>() {

    @Throws(IOException::class)
    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        if (exc != null) {
            throw exc
        }

        onDirectoryFoundListener.accept(dir)
        return FileVisitResult.CONTINUE
    }
}
