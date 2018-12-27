package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mapStatistics")
class MapStatistics {
    @Id
    private val id: String? = null
    private val downloads: Int = 0
    private val draws: Int = 0
    private val plays: Int = 0

    @Relationship("map")
    private val map: Map? = null
}
