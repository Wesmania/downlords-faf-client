package com.faforever.client.game

import java.util.HashMap

/**
 * An enumeration of "known" featured mods. They might be added and removed to the server arbitrarily, which is why
 * the client should rely as little as possible on this static definition.
 */
enum class KnownFeaturedMod private constructor(val technicalName: String) {
    FAF("faf"),
    FAF_BETA("fafbeta"),
    FAF_DEVELOP("fafdevelop"),
    BALANCE_TESTING("balancetesting"),
    LADDER_1V1("ladder1v1"),
    COOP("coop"),
    GALACTIC_WAR("gw"),
    MATCHMAKER("matchmaker");


    companion object {

        val DEFAULT = FAF

        private val fromString: MutableMap<String, KnownFeaturedMod>

        init {
            fromString = HashMap()
            for (knownFeaturedMod in values()) {
                fromString[knownFeaturedMod.technicalName] = knownFeaturedMod
            }
        }

        fun fromString(string: String): KnownFeaturedMod {
            return fromString[string]
        }
    }
}
