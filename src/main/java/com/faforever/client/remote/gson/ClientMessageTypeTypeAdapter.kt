package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.ClientMessageType
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class ClientMessageTypeTypeAdapter private constructor() : TypeAdapter<ClientMessageType>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: ClientMessageType?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): ClientMessageType {
        return ClientMessageType.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = ClientMessageTypeTypeAdapter()
    }
}
