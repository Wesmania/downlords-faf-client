package com.faforever.client.os

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Scanner
import java.util.function.Consumer

import com.github.nocatch.NoCatch.noCatch
import java.nio.charset.StandardCharsets.UTF_8

class OsUtils private constructor() {

    init {
        throw AssertionError("Not instantiable")
    }

    companion object {

        @Throws(IOException::class)
        fun execAndGetOutput(cmd: String): String {
            val scanner = Scanner(
                    Runtime.getRuntime().exec(cmd).inputStream, UTF_8.name()
            ).useDelimiter("\\A")
            return if (scanner.hasNext()) scanner.next().trim { it <= ' ' } else ""
        }

        fun gobbleLines(stream: InputStream, lineConsumer: Consumer<String>) {
            val thread = Thread {
                noCatch {
                    BufferedReader(InputStreamReader(stream)).use { bufferedReader ->
                        var line: String
                        while ((line = bufferedReader.readLine()) != null) {
                            lineConsumer.accept(line)
                        }
                    }
                }
            }
            thread.isDaemon = true
            thread.start()
        }
    }
}
