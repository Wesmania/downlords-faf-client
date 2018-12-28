package com.faforever.client.remote.domain

class GameInfoMessage(val uid: Int): FafServerMessage(FafServerMessageType.GAME_INFO) {
    val host: String? = null
    val passwordProtected: Boolean? = null
    // TODO use enum
    val visibility: String? = null
    val state: GameStatus? = null
    val numPlayers: Int? = null
    val teams: Map<String, List<String>>? = null
    val featuredModVersions: Map<String, Int>? = null
    val featuredMod: String? = null
    val maxPlayers: Int? = null
    val title: String? = null
    // FAF calls this "game_type" but it's actually the victory condition.
    val gameType: VictoryCondition? = null
    val simMods: Map<String, String>? = null
    val mapname: String? = null
    val launchedAt: Double? = null
    /**
     * The server may either send a single game or a list of games in the same message... *cringe*.
     */
    val games: List<GameInfoMessage>? = null

    override fun toString(): String {
        return "GameInfo{" +
                "uid=" + uid +
                ", title='" + title + '\''.toString() +
                ", state=" + state +
                '}'.toString()
    }

    fun isFull() = host != null &&
                   passwordProtected != null &&
                   visibility != null &&
                   state != null &&
                   numPlayers != null &&
                   teams != null &&
                   featuredModVersions != null &&
                   featuredMod != null &&
                   maxPlayers != null &&
                   title != null &&
                   gameType != null &&
                   simMods != null &&
                   mapname != null
}
