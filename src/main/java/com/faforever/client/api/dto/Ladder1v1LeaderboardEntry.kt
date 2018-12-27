package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("ladder1v1LeaderboardEntry")
class Ladder1v1LeaderboardEntry {
    @Id
    private val id: String? = null
    private val rank: Int = 0
    private val name: String? = null
    private val mean: Double? = null
    private val deviation: Double? = null
    private val numGames: Integer? = null
    private val wonGames: Integer? = null
    private val isActive: Boolean? = null
    private val rating: Double? = null
}
