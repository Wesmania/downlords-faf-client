package com.faforever.client.rankedmatch

import com.faforever.client.game.Faction
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.remote.domain.ClientMessageType

class SearchLadder1v1ClientMessage(val faction: Faction) : MatchMakerClientMessage(ClientMessageType.GAME_MATCH_MAKING) {

    init {
        mod = KnownFeaturedMod.LADDER_1V1.technicalName
        state = "start"
    }
}
