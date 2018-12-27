package com.faforever.client.remote.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.HashMap

enum class VictoryCondition private constructor(val value: Any) {
    // Order is crucial
    DEMORALIZATION(0),
    DOMINATION(1),
    ERADICATION(2),
    SANDBOX(3),
    UNKNOWN("unknown");


    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val fromNumber: MutableMap<Any, VictoryCondition>

        init {
            fromNumber = HashMap()
            for (victoryCondition in values()) {
                fromNumber[victoryCondition.value] = victoryCondition
            }
        }

        fun fromNumber(number: Any): VictoryCondition {
            val victoryCondition = fromNumber[number]
            if (victoryCondition == null) {
                logger.warn("Unknown victory condition: {}", number)
                return UNKNOWN
            }
            return victoryCondition
        }
    }
}
