package com.faforever.client.io

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import java.nio.file.Files.walkFileTree

class FileUtils private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        @Throws(IOException::class)
        fun deleteRecursively(path: Path) {
            walkFileTree(path, object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
                    if (e == null) {
                        Files.delete(dir)
                        return FileVisitResult.CONTINUE
                    } else {
                        throw e
                    }
                }
            })
        }
    }
}
