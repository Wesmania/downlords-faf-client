package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("mapVersionReviewsSummary")
class MapVersionReviewsSummary : ReviewsSummary() {

    @Relationship("MapVersion")
    var mapVersion: MapVersion? = null

}
