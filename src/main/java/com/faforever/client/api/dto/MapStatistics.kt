package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("mapStatistics")
data class MapStatistics(@Id var id: String? = null) {
    var downloads: Int = 0
    var draws: Int = 0
    var plays: Int = 0

    @Relationship("map")
    var map: Map? = null
}
