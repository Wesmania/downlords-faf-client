package com.faforever.client.remote

import com.faforever.client.api.FafApiAccessor
import com.faforever.client.api.dto.*
import com.faforever.client.chat.avatar.AvatarBean
import com.faforever.client.chat.avatar.event.AvatarChangedEvent
import com.faforever.client.clan.Clan
import com.faforever.client.config.CacheNames
import com.faforever.client.coop.CoopMission
import com.faforever.client.domain.RatingHistoryDataPoint
import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.game.Faction
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.game.NewGameInfo
import com.faforever.client.leaderboard.LeaderboardEntry
import com.faforever.client.map.MapBean
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModVersion
import com.faforever.client.net.ConnectionState
import com.faforever.client.player.Player
import com.faforever.client.remote.domain.GameEndedMessage
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.IceMessage
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.remote.domain.ServerMessage
import com.faforever.client.replay.Replay
import com.faforever.client.tournament.TournamentBean
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.io.ByteCountListener
import com.google.common.eventbus.EventBus
import javafx.beans.property.ReadOnlyObjectProperty
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.Assert

import javax.inject.Inject
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import java.util.stream.Collectors.toList

@Lazy
@Service
class FafService @Inject
constructor(private val fafServerAccessor: FafServerAccessor, private val fafApiAccessor: FafApiAccessor, private val eventBus: EventBus) {

    val mods: CompletableFuture<List<ModVersion>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.mods.stream()
                .map { ModVersion.fromModDto(it) }
                .collect(toList()))

    val coopMaps: CompletableFuture<List<CoopMission>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.coopMissions.stream()
                .map { CoopMission.fromCoopInfo(it) }
                .collect(toList()))

    val availableAvatars: CompletableFuture<List<AvatarBean>>
        @Async
        get() = CompletableFuture.completedFuture(fafServerAccessor.availableAvatars.stream()
                .map { AvatarBean.fromAvatar(it) }
                .collect(Collectors.toList()))

    val featuredMods: CompletableFuture<List<FeaturedMod>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.featuredMods.stream()
                .sorted(compareBy { it.order })
                .map { FeaturedMod.fromFeaturedMod(it) }
                .collect(Collectors.toList()))

    val ladder1v1Leaderboard: CompletableFuture<List<LeaderboardEntry>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.ladder1v1Leaderboard.parallelStream()
                .map { LeaderboardEntry.fromLadder1v1(it) }
                .collect(toList()))

    val globalLeaderboard: CompletableFuture<List<LeaderboardEntry>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.globalLeaderboard.parallelStream()
                .map { LeaderboardEntry.fromGlobalRating(it) }
                .collect(toList()))

    val achievementDefinitions: CompletableFuture<List<AchievementDefinition>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.achievementDefinitions)

    val iceServers: CompletableFuture<List<IceServer>>
        get() = fafServerAccessor.iceServers

    val allTournaments: CompletableFuture<List<TournamentBean>>
        @Async
        get() = CompletableFuture.completedFuture(fafApiAccessor.allTournaments
                .stream()
                .map { TournamentBean.fromTournamentDto(it) }
                .collect(toList()))

    fun <T : ServerMessage> addOnMessageListener(type: Class<T>, listener: (T) -> Unit) {
        fafServerAccessor.addOnMessageListener(type, listener)
    }

    fun <T : ServerMessage> removeOnMessageListener(type: Class<T>, listener: (T) -> Unit) {
        fafServerAccessor.removeOnMessageListener(type, listener)
    }

    fun requestHostGame(newGameInfo: NewGameInfo): CompletableFuture<GameLaunchMessage> {
        return fafServerAccessor.requestHostGame(newGameInfo)
    }

    fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState> {
        return fafServerAccessor.connectionStateProperty()
    }

    fun requestJoinGame(gameId: Int, password: String): CompletableFuture<GameLaunchMessage> {
        return fafServerAccessor.requestJoinGame(gameId, password)
    }

    fun startSearchLadder1v1(faction: Faction, port: Int): CompletableFuture<GameLaunchMessage> {
        return fafServerAccessor.startSearchLadder1v1(faction)
    }

    fun stopSearchingRanked() {
        fafServerAccessor.stopSearchingRanked()
    }

    fun sendGpgGameMessage(message: GpgGameMessage) {
        fafServerAccessor.sendGpgMessage(message)
    }

    fun connectAndLogIn(username: String, password: String): CompletableFuture<LoginMessage> {
        return fafServerAccessor.connectAndLogIn(username, password)
    }

    fun disconnect() {
        fafServerAccessor.disconnect()
    }

    fun addFriend(player: Player) {
        fafServerAccessor.addFriend(player.id)
    }

    fun addFoe(player: Player) {
        fafServerAccessor.addFoe(player.id)
    }

    fun removeFriend(player: Player) {
        fafServerAccessor.removeFriend(player.id)
    }

    fun removeFoe(player: Player) {
        fafServerAccessor.removeFoe(player.id)
    }

    @Async
    fun notifyGameEnded() {
        fafServerAccessor.sendGpgMessage(GameEndedMessage())
    }

    @Async
    fun getLadder1v1EntryForPlayer(playerId: Int): CompletableFuture<LeaderboardEntry> {
        return CompletableFuture.completedFuture(LeaderboardEntry.fromLadder1v1(fafApiAccessor.getLadder1v1EntryForPlayer(playerId)))
    }

    @Async
    fun getModVersion(uid: String): CompletableFuture<com.faforever.client.mod.ModVersion> {
        return CompletableFuture.completedFuture(com.faforever.client.mod.ModVersion.fromDto(fafApiAccessor.getModVersion(uid), null))
    }

    fun reconnect() {
        fafServerAccessor.reconnect()
    }

    @Async
    fun getMostPlayedMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getMostPlayedMaps(count, page).stream()
                .map { MapBean.fromMapDto(it) }
                .collect(toList()))
    }

    @Async
    fun getHighestRatedMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedMaps(count, page).stream()
                .map { MapBean.fromMapDto(it) }
                .collect(toList()))
    }

    @Async
    fun getNewestMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getNewestMaps(count, page).stream()
                .map { MapBean.fromMapDto(it) }
                .collect(toList()))
    }

    fun selectAvatar(avatar: AvatarBean?) {
        fafServerAccessor.selectAvatar(avatar?.url)
        eventBus.post(AvatarChangedEvent(avatar))
    }

    @CacheEvict(CacheNames.MODS)
    fun evictModsCache() {
        // Cache eviction by annotation
    }

    @Async
    fun getCoopLeaderboard(mission: CoopMission, numberOfPlayers: Int): CompletableFuture<List<CoopResult>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getCoopLeaderboard(mission.id, numberOfPlayers))
    }

    @Async
    fun getRatingHistory(playerId: Int, knownFeaturedMod: KnownFeaturedMod): CompletableFuture<List<RatingHistoryDataPoint>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getGamePlayerStats(playerId, knownFeaturedMod)
                .parallelStream()
                .filter { gamePlayerStats ->
                    (gamePlayerStats.scoreTime != null
                            && gamePlayerStats.afterMean != null
                            && gamePlayerStats.afterDeviation != null)
                }
                .sorted(compareBy { it.scoreTime })
                .map { entry -> RatingHistoryDataPoint(entry.scoreTime, entry.afterMean, entry.afterDeviation) }
                .collect(Collectors.toList())
        )
    }

    @Async
    fun getFeaturedModFiles(featuredMod: FeaturedMod, version: Int?): CompletableFuture<List<FeaturedModFile>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedModFiles(featuredMod, version))
    }

    @Async
    fun getNewestReplays(topElementCount: Int, page: Int): CompletableFuture<List<Replay>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getNewestReplays(topElementCount, page)
                .parallelStream()
                .map { Replay.fromDto(it) }
                .collect(toList()))
    }

    @Async
    fun getHighestRatedReplays(topElementCount: Int, page: Int): CompletableFuture<List<Replay>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedReplays(topElementCount, page)
                .parallelStream()
                .map { Replay.fromDto(it) }
                .collect(toList()))
    }

    fun uploadMod(modFile: Path, byteListener: ByteCountListener) {
        fafApiAccessor.uploadMod(modFile, byteListener)
    }

    @Async
    fun getPlayerAchievements(playerId: Int): CompletableFuture<List<PlayerAchievement>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getPlayerAchievements(playerId))
    }

    @Async
    fun getAchievementDefinition(achievementId: String): CompletableFuture<AchievementDefinition> {
        return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinition(achievementId))
    }

    @Async
    fun findReplaysByQuery(query: String, maxResults: Int, page: Int, sortConfig: SortConfig): CompletableFuture<List<Replay>> {
        return CompletableFuture.completedFuture(fafApiAccessor.findReplaysByQuery(query, maxResults, page, sortConfig)
                .parallelStream()
                .map { Replay.fromDto(it) }
                .collect(toList()))
    }

    @Async
    fun findMapsByQuery(query: SearchConfig, page: Int, count: Int): CompletableFuture<List<MapBean>> {
        return CompletableFuture.completedFuture(fafApiAccessor.findMapsByQuery(query, page, count)
                .parallelStream()
                .map { MapBean.fromMapDto(it) }
                .collect(toList()))
    }

    fun findMapByFolderName(folderName: String): CompletableFuture<Optional<MapBean>> {
        return CompletableFuture.completedFuture(fafApiAccessor.findMapByFolderName(folderName)
                .map { MapBean.fromMapVersionDto(it) })
    }

    fun getPlayersByIds(playerIds: Collection<Int>): CompletableFuture<List<Player>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getPlayersByIds(playerIds).stream()
                .map { Player.fromDto(it) }
                .collect(toList()))
    }

    @Async
    fun saveGameReview(review: Review, gameId: Int): CompletableFuture<Void> {
        val gameReview = GameReview()
                .setScore(review.score!!.toByte())
                .setText(review.text) as GameReview

        if (review.id == null) {
            Assert.notNull(review.player, "Player ID must be set")
            val updatedReview = fafApiAccessor.createGameReview(
                    gameReview
                            .setGame(Game().setId(gameId.toString()))
                            .setPlayer(com.faforever.client.api.dto.Player().setId(review.player.id.toString())) as GameReview
            )
            review.id = updatedReview.getId()
        } else {
            fafApiAccessor.updateGameReview(gameReview.setId(review.id.toString()) as GameReview)
        }
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun saveModVersionReview(review: Review, modVersionId: String): CompletableFuture<Void> {
        val modVersionReview = ModVersionReview()
                .setScore(review.score!!.toByte())
                .setText(review.text) as ModVersionReview

        if (review.id == null) {
            Assert.notNull(review.player, "Player ID must be set")
            val updatedReview = fafApiAccessor.createModVersionReview(
                    modVersionReview
                            .setModVersion(com.faforever.client.api.dto.ModVersion().setId(modVersionId))
                            .setId(review.id.toString())
                            .setPlayer(com.faforever.client.api.dto.Player().setId(review.player.id.toString())) as ModVersionReview
            )
            review.id = updatedReview.getId()
        } else {
            fafApiAccessor.updateModVersionReview(modVersionReview.setId(review.id.toString()) as ModVersionReview)
        }
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun saveMapVersionReview(review: Review, mapVersionId: String): CompletableFuture<Void> {
        val mapVersionReview = MapVersionReview()
                .setScore(review.score!!.toByte())
                .setText(review.text) as MapVersionReview

        if (review.id == null) {
            Assert.notNull(review.player, "Player ID must be set")
            val updatedReview = fafApiAccessor.createMapVersionReview(
                    mapVersionReview
                            .setMapVersion(MapVersion().setId(mapVersionId))
                            .setId(review.id.toString())
                            .setPlayer(com.faforever.client.api.dto.Player().setId(review.player.id.toString())) as MapVersionReview
            )
            review.id = updatedReview.getId()
        } else {
            fafApiAccessor.updateMapVersionReview(mapVersionReview.setId(review.id.toString()) as MapVersionReview)
        }

        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun getLastGameOnMap(playerId: Int, mapVersionId: String): CompletableFuture<Optional<Replay>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getLastGamesOnMap(playerId, mapVersionId, 1).stream()
                .map { Replay.fromDto(it) }
                .findFirst())
    }

    @Async
    fun deleteGameReview(review: Review): CompletableFuture<Void> {
        fafApiAccessor.deleteGameReview(review.id)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun deleteMapVersionReview(review: Review): CompletableFuture<Void> {
        fafApiAccessor.deleteMapVersionReview(review.id)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun deleteModVersionReview(review: Review): CompletableFuture<Void> {
        fafApiAccessor.deleteModVersionReview(review.id)
        return CompletableFuture.completedFuture(null)
    }

    fun findReplayById(id: Int): CompletableFuture<Optional<Replay>> {
        return CompletableFuture.completedFuture(fafApiAccessor.findReplayById(id)
                .map { Replay.fromDto(it) })
    }

    fun restoreGameSession(id: Int) {
        fafServerAccessor.restoreGameSession(id)
    }

    @Async
    fun findModsByQuery(query: SearchConfig, page: Int, count: Int): CompletableFuture<List<ModVersion>> {
        return CompletableFuture.completedFuture(fafApiAccessor.findModsByQuery(query, page, count)
                .parallelStream()
                .map { ModVersion.fromModDto(it) }
                .collect(toList()))
    }

    @Async
    fun getLadder1v1Maps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        val maps = fafApiAccessor.getLadder1v1Maps(count, page).stream()
                .map { ladder1v1Map -> MapBean.fromMapVersionDto(ladder1v1Map.mapVersion) }
                .collect(toList())
        return CompletableFuture.completedFuture(maps)
    }

    @Async
    fun getClanByTag(tag: String): CompletableFuture<Optional<Clan>> {
        return CompletableFuture.completedFuture(fafApiAccessor.getClanByTag(tag)
                .map { Clan.fromDto(it) })
    }

    fun findMapById(id: String): Optional<MapBean> {
        return fafApiAccessor.findMapVersionById(id)
                .map { MapBean.fromMapVersionDto(it) }
    }

    fun sendIceMessage(remotePlayerId: Int, message: Any) {
        fafServerAccessor.sendGpgMessage(IceMessage(remotePlayerId, message))
    }


    @Async
    fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): CompletableFuture<List<MapBean>> {
        val maps = fafApiAccessor.getOwnedMaps(playerId, loadMoreCount, page)
        return CompletableFuture.completedFuture(maps.stream().map { MapBean.fromMapVersionDto(it) }.collect(toList()))
    }

    @Async
    fun hideMapVersion(map: MapBean): CompletableFuture<Void> {
        val id = map.id
        val mapVersion = MapVersion()
        mapVersion.hidden = true
        mapVersion.id = map.id
        fafApiAccessor.updateMapVersion(id, mapVersion)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun unrankeMapVersion(map: MapBean): CompletableFuture<Void> {
        val id = map.id
        val mapVersion = MapVersion()
        mapVersion.ranked = false
        mapVersion.id = map.id
        fafApiAccessor.updateMapVersion(id, mapVersion)
        return CompletableFuture.completedFuture(null)
    }
}
