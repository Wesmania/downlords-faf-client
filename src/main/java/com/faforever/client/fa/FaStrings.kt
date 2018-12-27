package com.faforever.client.fa

import lombok.experimental.UtilityClass

@UtilityClass
class FaStrings {

    fun removeLocalizationTag(description: String): String {
        return description.replace("<LOC .*?>".toRegex(), "")
    }
}
