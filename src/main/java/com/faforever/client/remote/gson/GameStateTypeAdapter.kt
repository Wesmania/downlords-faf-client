package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.GameStatus
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class GameStateTypeAdapter private constructor()// private
    : TypeAdapter<GameStatus>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: GameStatus?) {
        if (value == null) {
            out.value(GameStatus.UNKNOWN.string)
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): GameStatus {
        return GameStatus.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = GameStateTypeAdapter()
    }
}
