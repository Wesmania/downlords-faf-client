package com.faforever.client.preferences.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class PathTypeAdapter private constructor() : TypeAdapter<Path>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Path?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toAbsolutePath().toString())
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Path? {
        val string = `in`.nextString()
        return if (string == null) {
            null
        } else {
            Paths.get(string)
        }
    }

    companion object {

        val INSTANCE = PathTypeAdapter()
    }
}
