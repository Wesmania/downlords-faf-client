package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Getter
import lombok.Setter

@Type("modVersionReviewsSummary")
@Getter
@Setter
class ModVersionReviewsSummary : ReviewsSummary() {

    @Relationship("modVersion")
    private val modVersion: ModVersion? = null
}
