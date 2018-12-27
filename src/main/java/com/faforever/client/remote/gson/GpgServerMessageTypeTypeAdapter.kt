package com.faforever.client.remote.gson

import com.faforever.client.fa.relay.GpgServerMessageType
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class GpgServerMessageTypeTypeAdapter private constructor() : TypeAdapter<GpgServerMessageType>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: GpgServerMessageType) {
        out.value(value.string)
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): GpgServerMessageType {
        return GpgServerMessageType.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = GpgServerMessageTypeTypeAdapter()
    }
}
