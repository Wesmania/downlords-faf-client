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
@Type("modVersion")
class ModVersion {

    @Id
    private val id: String? = null
    private val uid: String? = null
    private val type: ModType? = null
    private val description: String? = null
    private val version: ComparableVersion? = null
    private val filename: String? = null
    private val icon: String? = null
    private val ranked: Boolean = false
    private val hidden: Boolean = false
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null
    private val thumbnailUrl: URL? = null
    private val downloadUrl: URL? = null

    @Relationship("mod")
    private val mod: Mod? = null

    @Relationship("reviews")
    private val reviews: List<ModVersionReview>? = null

    @Relationship("reviewsSummary")
    private val modVersionReviewsSummary: ModVersionReviewsSummary? = null
}
