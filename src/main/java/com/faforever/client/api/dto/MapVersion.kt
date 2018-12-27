package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter
import org.apache.maven.artifact.versioning.ComparableVersion

import java.net.URL
import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mapVersion")
class MapVersion {

    @Id
    private val id: String? = null
    private val description: String? = null
    private val maxPlayers: Integer? = null
    private val width: Integer? = null
    private val height: Integer? = null
    private val version: ComparableVersion? = null
    private val folderName: String? = null
    // TODO name consistently with folderName
    private val filename: String? = null
    private val ranked: Boolean? = null
    private val hidden: Boolean? = null
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null
    private val thumbnailUrlSmall: URL? = null
    private val thumbnailUrlLarge: URL? = null
    private val downloadUrl: URL? = null

    @Relationship("map")
    private val map: Map? = null

    @Relationship("statistics")
    private val statistics: MapVersionStatistics? = null

    @Relationship("reviews")
    private val reviews: List<MapVersionReview>? = null
}
