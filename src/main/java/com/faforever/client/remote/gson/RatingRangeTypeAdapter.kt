package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.RatingRange
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class RatingRangeTypeAdapter private constructor() : TypeAdapter<RatingRange>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: RatingRange) {
        out.beginArray()
        out.value(value.min)
        out.value(value.max)
        out.endArray()
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): RatingRange {
        `in`.beginArray()
        val min = `in`.nextInt()
        val max = `in`.nextInt()
        `in`.endArray()
        return RatingRange(min, max)
    }

    companion object {

        val INSTANCE = RatingRangeTypeAdapter()
    }
}
