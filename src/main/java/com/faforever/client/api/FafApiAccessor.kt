package com.faforever.client.api

import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.api.dto.Clan
import com.faforever.client.api.dto.CoopMission
import com.faforever.client.api.dto.CoopResult
import com.faforever.client.api.dto.FeaturedModFile
import com.faforever.client.api.dto.Game
import com.faforever.client.api.dto.GamePlayerStats
import com.faforever.client.api.dto.GameReview
import com.faforever.client.api.dto.GlobalLeaderboardEntry
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry
import com.faforever.client.api.dto.Ladder1v1Map
import com.faforever.client.api.dto.Map
import com.faforever.client.api.dto.MapVersion
import com.faforever.client.api.dto.MapVersionReview
import com.faforever.client.api.dto.Mod
import com.faforever.client.api.dto.ModVersion
import com.faforever.client.api.dto.ModVersionReview
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.api.dto.PlayerEvent
import com.faforever.client.api.dto.Tournament
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.io.ByteCountListener

import java.io.IOException
import java.nio.file.Path
import java.util.Optional

/**
 * Provides access to the FAF REST API. Services should not access this class directly, but use [ ] instead.
 */
interface FafApiAccessor {

    val achievementDefinitions: List<AchievementDefinition>

    val mods: List<Mod>

    val featuredMods: List<com.faforever.client.api.dto.FeaturedMod>

    val ladder1v1Leaderboard: List<Ladder1v1LeaderboardEntry>

    val globalLeaderboard: List<GlobalLeaderboardEntry>

    val coopMissions: List<CoopMission>

    val allTournaments: List<Tournament>

    fun getPlayerAchievements(playerId: Int): List<PlayerAchievement>

    @SuppressWarnings("unchecked")
    fun getPlayerEvents(playerId: Int): List<PlayerEvent>

    fun getAchievementDefinition(achievementId: String): AchievementDefinition

    fun authorize(playerId: Int, username: String, password: String)

    fun getLadder1v1EntryForPlayer(playerId: Int): Ladder1v1LeaderboardEntry

    fun getGamePlayerStats(playerId: Int, knownFeaturedMod: KnownFeaturedMod): List<GamePlayerStats>

    fun getMostPlayedMaps(count: Int, page: Int): List<Map>

    fun getHighestRatedMaps(count: Int, page: Int): List<Map>

    fun getNewestMaps(count: Int, page: Int): List<Map>

    fun getLastGamesOnMap(playerId: Int, mapVersionId: String, count: Int): List<Game>

    fun uploadMod(file: Path, listener: ByteCountListener)

    @Throws(IOException::class)
    fun uploadMap(file: Path, isRanked: Boolean, listener: ByteCountListener)

    fun getCoopLeaderboard(missionId: String, numberOfPlayers: Int): List<CoopResult>

    @Throws(IOException::class)
    fun changePassword(username: String, currentPasswordHash: String, newPasswordHash: String)

    fun getModVersion(uid: String): ModVersion

    fun getFeaturedModFiles(featuredMod: FeaturedMod, version: Integer): List<FeaturedModFile>

    fun getNewestReplays(count: Int, page: Int): List<Game>

    fun getHighestRatedReplays(count: Int, page: Int): List<Game>

    fun findReplaysByQuery(condition: String, maxResults: Int, page: Int, sortConfig: SortConfig): List<Game>

    fun findMapByFolderName(folderName: String): Optional<MapVersion>

    fun getPlayersByIds(playerIds: Collection<Integer>): List<com.faforever.client.api.dto.Player>

    fun createGameReview(review: GameReview): GameReview

    fun updateGameReview(review: GameReview)

    fun createModVersionReview(review: ModVersionReview): ModVersionReview

    fun updateModVersionReview(review: ModVersionReview)

    fun createMapVersionReview(review: MapVersionReview): MapVersionReview

    fun updateMapVersionReview(review: MapVersionReview)

    fun deleteGameReview(id: String)

    fun getClanByTag(tag: String): Optional<Clan>

    fun findMapsByQuery(searchConfig: SearchConfig, page: Int, count: Int): List<Map>

    fun findMapVersionById(id: String): Optional<MapVersion>

    fun deleteMapVersionReview(id: String)

    fun deleteModVersionReview(id: String)

    fun findReplayById(id: Int): Optional<Game>

    fun findModsByQuery(query: SearchConfig, page: Int, maxResults: Int): List<Mod>

    fun getLadder1v1Maps(count: Int, page: Int): List<Ladder1v1Map>

    fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): List<MapVersion>

    fun updateMapVersion(id: String, mapVersion: MapVersion)
}
