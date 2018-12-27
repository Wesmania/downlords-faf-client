package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("coopMission")
class CoopMission {
    @Id
    private val id: String? = null
    private val name: String? = null
    private val version: Int = 0
    private val category: String? = null
    private val thumbnailUrlSmall: String? = null
    private val thumbnailUrlLarge: String? = null
    private val description: String? = null
    private val downloadUrl: String? = null
    private val folderName: String? = null
}
