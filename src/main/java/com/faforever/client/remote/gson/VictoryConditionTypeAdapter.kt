package com.faforever.client.remote.gson

import com.faforever.client.remote.domain.VictoryCondition
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.IOException

class VictoryConditionTypeAdapter private constructor() : TypeAdapter<VictoryCondition>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, victoryCondition: VictoryCondition?) {
        if (victoryCondition == null) {
            out.value("unknown")
        } else {
            val value = victoryCondition.value
            if (value is Int) {
                out.value(value.toLong())
            } else {
                out.value(value as String)
            }
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): VictoryCondition {
        val victoryCondition = `in`.nextString()
        return if (victoryCondition == null || "unknown" == victoryCondition) {
            VictoryCondition.UNKNOWN
        } else VictoryCondition.fromNumber(Integer.valueOf(victoryCondition))
    }

    companion object {

        val INSTANCE = VictoryConditionTypeAdapter()
    }
}
