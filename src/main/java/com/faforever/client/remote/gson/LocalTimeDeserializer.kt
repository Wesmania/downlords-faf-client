package com.faforever.client.remote.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement

import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalTimeDeserializer private constructor() : JsonDeserializer<LocalTime> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime {
        return LocalTime.parse(json.asString, FORMATTER)
    }

    companion object {

        val INSTANCE = LocalTimeDeserializer()
        private val FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
}
