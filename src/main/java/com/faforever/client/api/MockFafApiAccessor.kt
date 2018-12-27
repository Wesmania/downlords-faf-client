package com.faforever.client.api

import com.faforever.client.FafClientApplication
import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.api.dto.AchievementType
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
import com.faforever.client.api.dto.Player
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.api.dto.PlayerEvent
import com.faforever.client.api.dto.Tournament
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.io.ByteCountListener
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.Arrays
import java.util.Collections
import java.util.Optional

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
// NOSONAR
class MockFafApiAccessor : FafApiAccessor {

    val achievementDefinitions: List<AchievementDefinition>
        @Override
        get() = Collections.emptyList()

    val mods: List<Mod>
        @Override
        get() {
            val uploader = Player()
            return Arrays.asList(
                    com.faforever.client.api.dto.Mod("1", "Mod Number One", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("2", "Mod Number Two", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("3", "Mod Number Three", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("4", "Mod Number Four", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("5", "Mod Number Five", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("6", "Mod Number Six", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("7", "Mod Number Seven", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
                    com.faforever.client.api.dto.Mod("8", "Mod Number Eight", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod())
            )
        }

    val featuredMods: List<com.faforever.client.api.dto.FeaturedMod>
        @Override
        get() {
            val featuredMod = com.faforever.client.api.dto.FeaturedMod()
            featuredMod.setDisplayName("Forged Alliance Forever")
            featuredMod.setTechnicalName("faf")
            featuredMod.setVisible(true)
            featuredMod.setDescription("Description")

            return Collections.singletonList(featuredMod)
        }

    val ladder1v1Leaderboard: List<Ladder1v1LeaderboardEntry>
        @Override
        get() = Collections.emptyList()

    val globalLeaderboard: List<GlobalLeaderboardEntry>
        @Override
        get() = Collections.emptyList()

    val coopMissions: List<CoopMission>
        @Override
        get() = Collections.emptyList()

    val allTournaments: List<Tournament>
        @Override
        get() = Collections.emptyList()

    @Override
    fun getPlayerAchievements(playerId: Int): List<PlayerAchievement> {
        return Collections.emptyList()
    }

    @Override
    fun getPlayerEvents(playerId: Int): List<PlayerEvent>? {
        return null
    }

    @Override
    fun getAchievementDefinition(achievementId: String): AchievementDefinition {
        val achievementDefinition = AchievementDefinition()
        achievementDefinition.setName("Mock achievement")
        achievementDefinition.setDescription("Congratulations! You read this text.")
        achievementDefinition.setType(AchievementType.STANDARD)
        return achievementDefinition
    }

    @Override
    fun authorize(playerId: Int, username: String, password: String) {

    }

    private fun mod(): ModVersion {
        return ModVersion()
    }

    @Override
    fun getLadder1v1EntryForPlayer(playerId: Int): Ladder1v1LeaderboardEntry? {
        return null
    }

    @Override
    fun getGamePlayerStats(playerId: Int, knownFeaturedMod: KnownFeaturedMod): List<GamePlayerStats> {
        return Collections.emptyList()
    }

    @Override
    fun getMostPlayedMaps(count: Int, page: Int): List<Map> {
        return Collections.emptyList()
    }

    @Override
    fun getHighestRatedMaps(count: Int, page: Int): List<Map> {
        return Collections.emptyList()
    }

    @Override
    fun getNewestMaps(count: Int, page: Int): List<Map> {
        return Collections.emptyList()
    }

    @Override
    fun getLastGamesOnMap(playerId: Int, mapVersionId: String, count: Int): List<Game> {
        return Collections.emptyList()
    }

    @Override
    fun uploadMod(file: Path, listener: ByteCountListener) {

    }

    @Override
    fun uploadMap(file: Path, isRanked: Boolean, listener: ByteCountListener) {

    }

    @Override
    fun getModVersion(uid: String): ModVersion? {
        return null
    }

    @Override
    fun getFeaturedModFiles(featuredMod: FeaturedMod, version: Integer): List<FeaturedModFile> {
        return Collections.emptyList()
    }

    @Override
    fun getNewestReplays(count: Int, page: Int): List<Game> {
        return Collections.emptyList()
    }

    @Override
    fun getHighestRatedReplays(count: Int, page: Int): List<Game> {
        return Collections.emptyList()
    }

    @Override
    fun findReplaysByQuery(query: String, maxResults: Int, page: Int, sortConfig: SortConfig): List<Game> {
        return Collections.emptyList()
    }

    @Override
    fun findMapByFolderName(folderName: String): Optional<MapVersion> {
        return Optional.empty()
    }

    @Override
    fun getPlayersByIds(playerIds: Collection<Integer>): List<com.faforever.client.api.dto.Player> {
        return Collections.emptyList()
    }

    @Override
    fun createGameReview(review: GameReview): GameReview? {
        return null
    }

    @Override
    fun updateGameReview(review: GameReview) {

    }

    @Override
    fun createModVersionReview(review: ModVersionReview): ModVersionReview? {
        return null
    }

    @Override
    fun updateModVersionReview(review: ModVersionReview) {

    }

    @Override
    fun createMapVersionReview(review: MapVersionReview): MapVersionReview? {
        return null
    }

    @Override
    fun updateMapVersionReview(review: MapVersionReview) {

    }

    @Override
    fun deleteGameReview(id: String) {

    }

    @Override
    fun getClanByTag(tag: String): Optional<Clan> {
        return Optional.empty()
    }

    @Override
    fun findMapsByQuery(searchConfig: SearchConfig, page: Int, count: Int): List<Map> {
        return Collections.emptyList()
    }

    @Override
    fun findMapVersionById(id: String): Optional<MapVersion> {
        return Optional.empty()
    }

    @Override
    fun deleteMapVersionReview(id: String) {

    }

    @Override
    fun deleteModVersionReview(id: String) {

    }

    @Override
    fun findReplayById(id: Int): Optional<Game> {
        return Optional.empty()
    }

    @Override
    fun findModsByQuery(query: SearchConfig, page: Int, maxResults: Int): List<Mod> {
        return Collections.emptyList()
    }

    @Override
    fun getLadder1v1Maps(count: Int, page: Int): List<Ladder1v1Map> {
        return Collections.emptyList()
    }

    @Override
    fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): List<MapVersion> {
        return Collections.emptyList()
    }

    @Override
    fun updateMapVersion(id: String, mapVersion: MapVersion) {
    }

    @Override
    fun changePassword(username: String, currentPasswordHash: String, newPasswordHash: String) {

    }

    @Override
    fun getCoopLeaderboard(missionId: String, numberOfPlayers: Int): List<CoopResult> {
        return Collections.emptyList()
    }
}
