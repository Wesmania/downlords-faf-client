package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("modVersionReviewsSummary")
class ModVersionReviewsSummary : ReviewsSummary() {

    @Relationship("modVersion")
    var modVersion: ModVersion? = null
}
