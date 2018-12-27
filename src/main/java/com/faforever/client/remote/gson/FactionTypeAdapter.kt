package com.faforever.client.remote.gson

import com.faforever.client.game.Faction
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import java.io.IOException

class FactionTypeAdapter private constructor()// private
    : TypeAdapter<Faction>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Faction) {
        out.value(value.string)
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Faction? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return Faction.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = FactionTypeAdapter()
    }
}
