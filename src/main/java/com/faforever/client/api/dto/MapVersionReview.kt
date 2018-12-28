package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("mapVersionReview")
class MapVersionReview : Review() {

    @Relationship("mapVersion")
    var mapVersion: MapVersion? = null
}
