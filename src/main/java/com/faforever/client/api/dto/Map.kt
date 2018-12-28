package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("map")
data class Map(@Id var id: String? = null) {

    var battleType: String? = null
    var createTime: OffsetDateTime? = null
    var updateTime: OffsetDateTime? = null
    var displayName: String? = null
    var mapType: String? = null

    @Relationship("author")
    var author: Player? = null

    @Relationship("statistics")
    var statistics: MapStatistics? = null

    @Relationship("latestVersion")
    var latestVersion: MapVersion? = null

    @Relationship("versions")
    var versions: List<MapVersion>? = null

    @Relationship("reviewsSummary")
    var mapVersionReviewsSummary: MapVersionReviewsSummary? = null
}
