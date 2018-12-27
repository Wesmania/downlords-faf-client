package com.faforever.client.remote.gson

import com.faforever.client.game.GameVisibility
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class GameVisibilityTypeAdapter private constructor() : TypeAdapter<GameVisibility>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: GameVisibility) {
        out.value(value.string)
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): GameVisibility {
        return GameVisibility.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = GameVisibilityTypeAdapter()
    }
}
