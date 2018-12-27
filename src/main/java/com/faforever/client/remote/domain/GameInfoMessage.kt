package com.faforever.client.remote.domain

import lombok.Getter
import lombok.Setter

@Getter
@Setter
class GameInfoMessage : FafServerMessage(FafServerMessageType.GAME_INFO) {

    private val host: String? = null
    private val passwordProtected: Boolean? = null
    // TODO use enum
    private val visibility: String? = null
    private val state: GameStatus? = null
    private val numPlayers: Int? = null
    private val teams: Map<String, List<String>>? = null
    private val featuredModVersions: Map<String, Int>? = null
    private val featuredMod: String? = null
    private val uid: Int? = null
    private val maxPlayers: Int? = null
    private val title: String? = null
    // FAF calls this "game_type" but it's actually the victory condition.
    private val gameType: VictoryCondition? = null
    private val simMods: Map<String, String>? = null
    private val mapname: String? = null
    private val launchedAt: Double? = null
    /**
     * The server may either send a single game or a list of games in the same message... *cringe*.
     */
    var games: List<GameInfoMessage>? = null

    override fun toString(): String {
        return "GameInfo{" +
                "uid=" + uid +
                ", title='" + title + '\''.toString() +
                ", state=" + state +
                '}'.toString()
    }
}
