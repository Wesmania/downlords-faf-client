package com.faforever.client.game

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

import java.util.HashMap

enum class Faction private constructor(
        /**
         * Returns the string value of the faction, as used in the game and the server.
         */
        val string: String) {
    // Order is crucial
    AEON("aeon"),
    CYBRAN("cybran"),
    UEF("uef"),
    SERAPHIM("seraphim"),
    NOMAD("nomad"),
    CIVILIAN("civilian");

    /**
     * Returns the faction value used as in "Forged Alliance Forever".
     */
    @JsonValue
    fun toFaValue(): Int {
        return ordinal + 1
    }

    companion object {

        private val fromString: MutableMap<String, Faction>

        init {
            fromString = HashMap()
            for (faction in values()) {
                fromString[faction.string] = faction
            }
        }

        @JsonCreator
        fun fromFaValue(value: Int): Faction {
            return Faction.values()[value - 1]
        }

        fun fromString(string: String): Faction {
            return fromString[string]
        }
    }
}
