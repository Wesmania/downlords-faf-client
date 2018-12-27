package com.faforever.client.util

import com.google.common.io.CharStreams
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

import java.nio.charset.StandardCharsets.UTF_8

class LuaUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        @Throws(IOException::class)
        fun loadFile(file: Path): LuaValue {
            Files.newInputStream(file).use { inputStream -> return load(inputStream) }
        }

        @Throws(IOException::class)
        fun load(inputStream: InputStream): LuaValue {
            val globals = JsePlatform.standardGlobals()
            globals.baselib.load(globals.load(CharStreams.toString(InputStreamReader(LuaUtil::class.java.getResourceAsStream("/lua/faf.lua"), UTF_8))))
            globals.load(inputStream, "@" + inputStream.hashCode(), "bt", globals).invoke()
            return globals
        }
    }
}
