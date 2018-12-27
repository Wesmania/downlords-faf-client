package com.faforever.client.preferences

import java.util.Locale

enum class TimeInfo private constructor(val displayNameKey: String, val usedLocale: Locale) {
    AUTO("settings.time.system", null),
    MILITARY_TIME("settings.time.24", Locale("de", "DE")),
    UK_TIME("settings.time.12", Locale("en", "UK"))
}

