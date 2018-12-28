package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("modVersionReview")
class ModVersionReview : Review() {

    @Relationship("modVersion")
    var modVersion: ModVersion? = null
}
