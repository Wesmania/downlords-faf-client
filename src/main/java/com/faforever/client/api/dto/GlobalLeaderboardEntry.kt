package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("globalLeaderboardEntry")
data class GlobalLeaderboardEntry(@Id var id: String? = null) {
    var name: String? = null
    var rank: Int = 0
    var mean: Double? = null
    var deviation: Double? = null
    var numGames: Integer? = null
    var isActive: Boolean? = null
    var rating: Double? = null
}
