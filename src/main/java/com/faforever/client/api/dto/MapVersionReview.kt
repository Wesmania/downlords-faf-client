package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@Type("mapVersionReview")
class MapVersionReview : Review() {

    @Relationship("mapVersion")
    private val mapVersion: MapVersion? = null
}
