package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode

@EqualsAndHashCode(of = ["id"])
@Type("coopMission")
data class CoopMission(@Id var id: String? = null) {
    var name: String? = null
    var version: Int = 0
    var category: String? = null
    var thumbnailUrlSmall: String? = null
    var thumbnailUrlLarge: String? = null
    var description: String? = null
    var downloadUrl: String? = null
    var folderName: String? = null
}
