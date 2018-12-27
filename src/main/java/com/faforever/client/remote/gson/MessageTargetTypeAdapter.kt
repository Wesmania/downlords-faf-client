package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.MessageTarget
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import java.io.IOException

class MessageTargetTypeAdapter private constructor() : TypeAdapter<MessageTarget>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: MessageTarget?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.string)
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): MessageTarget? {
        return if (`in`.peek() == JsonToken.NULL) {
            null
        } else MessageTarget.fromString(`in`.nextString())
    }

    companion object {

        val INSTANCE = MessageTargetTypeAdapter()
    }
}
