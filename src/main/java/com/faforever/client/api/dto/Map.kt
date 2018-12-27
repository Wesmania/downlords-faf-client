package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("map")
class Map {

    @Id
    private val id: String? = null
    private val battleType: String? = null
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null
    private val displayName: String? = null
    private val mapType: String? = null

    @Relationship("author")
    private val author: Player? = null

    @Relationship("statistics")
    private val statistics: MapStatistics? = null

    @Relationship("latestVersion")
    private val latestVersion: MapVersion? = null

    @Relationship("versions")
    private val versions: List<MapVersion>? = null

    @Relationship("reviewsSummary")
    private val mapVersionReviewsSummary: MapVersionReviewsSummary? = null
}
