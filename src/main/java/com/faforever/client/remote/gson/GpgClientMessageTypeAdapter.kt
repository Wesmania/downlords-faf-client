package com.faforever.client.remote.gson

import com.faforever.client.fa.relay.GpgClientCommand
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class GpgClientMessageTypeAdapter private constructor() : TypeAdapter<GpgClientCommand>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: GpgClientCommand?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): GpgClientCommand? {
        return GpgClientCommand.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = GpgClientMessageTypeAdapter()
    }
}
