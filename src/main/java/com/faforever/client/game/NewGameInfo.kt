package com.faforever.client.game

import com.faforever.client.mod.FeaturedMod
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
class NewGameInfo(title: String, password: String, featuredMod: FeaturedMod, map: String, simMods: Set<String>) {
    private val title: String? = null
    private val password: String? = null
    private val featuredMod: FeaturedMod? = null
    private val map: String? = null
    private val simMods: Set<String>? = null
    private val gameVisibility: GameVisibility? = null

    init {
        this(title, password, featuredMod, map, simMods, GameVisibility.PUBLIC)
    }
}
