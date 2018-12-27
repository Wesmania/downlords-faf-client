package com.faforever.client.remote.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException

import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateDeserializer private constructor() : JsonDeserializer<LocalDate> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
        return LocalDate.parse(json.asString, FORMATTER)
    }

    companion object {

        val INSTANCE = LocalDateDeserializer()
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    }
}
