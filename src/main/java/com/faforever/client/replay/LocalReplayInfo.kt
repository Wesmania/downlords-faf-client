package com.faforever.client.replay

import com.faforever.client.game.Game
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.VictoryCondition

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
class LocalReplayInfo {

    var host: String? = null
    var uid: Int? = null
    var title: String? = null
    var mapname: String? = null
    var state: GameStatus? = null
    var options: Array<Boolean>? = null
    // FAF calls this "game_type" but it's actually the victory condition.
    var gameType: VictoryCondition? = null
    var featuredMod: String? = null
    var maxPlayers: Int? = null
    var numPlayers: Int? = null
    var simMods: Map<String, String>? = null
    var teams: Map<String, List<String>>? = null
    var featuredModVersions: Map<String, Int>? = null
    var isComplete: Boolean = false
    var recorder: String? = null
    var versionInfo: Map<String, String>? = null
    var gameEnd: Double = 0.toDouble()
    /**
     * Backwards compatibility: If 0.0, then `launchedAt` should be available instead.
     */
    var gameTime: Double = 0.toDouble()
    /**
     * Backwards compatibility: If 0.0, then `gameTime` should be available instead.
     */
    var launchedAt: Double = 0.toDouble()

    fun updateFromGameInfoBean(game: Game) {
        host = game.host
        uid = game.id
        title = game.title
        mapname = game.mapFolderName
        state = game.status
        gameType = game.victoryCondition
        featuredMod = game.featuredMod
        maxPlayers = game.maxPlayers
        numPlayers = game.numPlayers
        simMods = game.simMods
        // FIXME this (and all others here) should do a deep copy
        teams = game.teams
        featuredModVersions = game.featuredModVersions
    }
}
