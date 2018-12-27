package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.GameAccess
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class GameAccessTypeAdapter private constructor() : TypeAdapter<GameAccess>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: GameAccess?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): GameAccess {
        return GameAccess.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = GameAccessTypeAdapter()
    }
}
