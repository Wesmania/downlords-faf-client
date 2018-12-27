package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("globalLeaderboardEntry")
class GlobalLeaderboardEntry {
    @Id
    private val id: String? = null
    private val name: String? = null
    private val rank: Int = 0
    private val mean: Double? = null
    private val deviation: Double? = null
    private val numGames: Integer? = null
    private val isActive: Boolean? = null
    private val rating: Double? = null
}
