package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import org.apache.maven.artifact.versioning.ComparableVersion

import java.net.URL
import java.time.OffsetDateTime

@Type("modVersion")
class ModVersion(@Id var id: String? = null) {
    var uid: String? = null
    var type: ModType? = null
    var description: String? = null
    var version: ComparableVersion? = null
    var filename: String? = null
    var icon: String? = null
    var ranked: Boolean = false
    var hidden: Boolean = false
    var createTime: OffsetDateTime? = null
    var updateTime: OffsetDateTime? = null
    var thumbnailUrl: URL? = null
    var downloadUrl: URL? = null

    @Relationship("mod")
    var mod: Mod? = null

    @Relationship("reviews")
    var reviews: List<ModVersionReview>? = null

    @Relationship("reviewsSummary")
    var modVersionReviewsSummary: ModVersionReviewsSummary? = null
}
