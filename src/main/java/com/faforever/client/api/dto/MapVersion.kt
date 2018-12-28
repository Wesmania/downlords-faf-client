package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import org.apache.maven.artifact.versioning.ComparableVersion

import java.net.URL
import java.time.OffsetDateTime

@Type("mapVersion")
data class MapVersion(@Id var id: String? = null) {
    var description: String? = null
    var maxPlayers: Int? = null
    var width: Int? = null
    var height: Int? = null
    var version: ComparableVersion? = null
    var folderName: String? = null
    // TODO name consistently with folderName
    var filename: String? = null
    var ranked: Boolean? = null
    var hidden: Boolean? = null
    var createTime: OffsetDateTime? = null
    var updateTime: OffsetDateTime? = null
    var thumbnailUrlSmall: URL? = null
    var thumbnailUrlLarge: URL? = null
    var downloadUrl: URL? = null

    @Relationship("map")
    var map: Map? = null

    @Relationship("statistics")
    var statistics: MapVersionStatistics? = null

    @Relationship("reviews")
    var reviews: List<MapVersionReview>? = null
}
