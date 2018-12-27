package com.faforever.client.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class TimeUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        fun fromPythonTime(time: Double): OffsetDateTime {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli((time * 1000).toLong()), ZoneId.systemDefault())
        }
    }
}
