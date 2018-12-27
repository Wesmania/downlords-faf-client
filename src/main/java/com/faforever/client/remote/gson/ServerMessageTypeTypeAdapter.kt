package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.FafServerMessageType
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class ServerMessageTypeTypeAdapter private constructor() : TypeAdapter<FafServerMessageType>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: FafServerMessageType?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): FafServerMessageType {
        return FafServerMessageType.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = ServerMessageTypeTypeAdapter()
    }
}
