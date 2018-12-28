package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("ladder1v1LeaderboardEntry")
data class Ladder1v1LeaderboardEntry(@Id var id: String? = null) {
    var rank: Int = 0
    var name: String? = null
    var mean: Double? = null
    var deviation: Double? = null
    var numGames: Integer? = null
    var wonGames: Integer? = null
    var isActive: Boolean? = null
    var rating: Double? = null
}
