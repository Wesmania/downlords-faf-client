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
import com.faforever.client.api.dto.MapStatistics
import com.faforever.client.api.dto.MapVersion
import com.faforever.client.api.dto.MapVersionReview
import com.faforever.client.api.dto.Mod
import com.faforever.client.api.dto.ModVersion
import com.faforever.client.api.dto.ModVersionReview
import com.faforever.client.api.dto.Player
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.api.dto.PlayerEvent
import com.faforever.client.api.dto.Tournament
import com.faforever.client.config.CacheNames
import com.faforever.client.config.ClientProperties
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.io.CountingFileSystemResource
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.faforever.client.util.getForObject
import com.faforever.client.util.mapToMultiMap
import com.faforever.client.util.typeRef
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.io.ByteCountListener
import com.github.rutledgepaulv.qbuilders.builders.QBuilder
import com.github.rutledgepaulv.qbuilders.conditions.Condition
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails
import org.springframework.security.oauth2.common.AuthenticationScheme
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.io.Serializable
import java.nio.file.Path
import java.util.LinkedList
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.stream.Collectors

@Slf4j
@Component
@Profile("!offline")
class FafApiAccessorImpl @Inject
constructor(private val eventBus: EventBus, restTemplateBuilder: RestTemplateBuilder,
            private val clientProperties: ClientProperties, jsonApiMessageConverter: JsonApiMessageConverter,
            jsonApiErrorHandler: JsonApiErrorHandler) : FafApiAccessor {
    private val restTemplateBuilder: RestTemplateBuilder
    private val requestFactory: HttpComponentsClientHttpRequestFactory = HttpComponentsClientHttpRequestFactory()

    private var authorizedLatch: CountDownLatch = CountDownLatch(1)
    private var restOperations: RestOperations? = null

    override val achievementDefinitions: List<AchievementDefinition>
        @SuppressWarnings("unchecked")
        @Cacheable(CacheNames.ACHIEVEMENTS)
        get() = getAll("/data/achievement", mapOf(
                "sort" to "order"
        ))

    override val mods: List<Mod>
        @Cacheable(CacheNames.MODS)
        get() = getAll("/data/mod", mapOf(
                "include" to "latestVersion,latestVersion.reviewsSummary"))

    override val featuredMods: List<com.faforever.client.api.dto.FeaturedMod>
        @Cacheable(CacheNames.FEATURED_MODS)
        get() = getMany("/data/featuredMod", 1000, emptyMap())

    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it's called manually
    override val globalLeaderboard: List<GlobalLeaderboardEntry>
        @Cacheable(CacheNames.GLOBAL_LEADERBOARD)
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() {
            authorizedLatch.await()
            return restOperations!!.getForObject("/leaderboards/global",
                    mapOf(
                            "sort" to "-rating",
                            "include" to "player",
                            "fields[globalRating]" to "rating,numGames",
                            "fields[player]" to "login"
                    ), typeRef<List<GlobalLeaderboardEntry>>()) ?: emptyList()
        }

    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it doesn't use getAll()
    override val ladder1v1Leaderboard: List<Ladder1v1LeaderboardEntry>
        @Cacheable(CacheNames.LADDER_1V1_LEADERBOARD)
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() {
            authorizedLatch.await()
            return restOperations!!.getForObject("/leaderboards/ladder1v1",
                    mapOf(
                            "sort" to "-rating",
                            "include" to "player",
                            "fields[ladder1v1Rating]" to "rating,numGames,winGames",
                            "fields[player]" to "login"
                    ), typeRef<List<Ladder1v1LeaderboardEntry>>()) ?: emptyList()
        }

    override val coopMissions: List<CoopMission>
        @Override
        @Cacheable(CacheNames.COOP_MAPS)
        get() = this.getAll("/data/coopMission")

    override val allTournaments: List<Tournament>
        @Override
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() = restOperations!!.getForObject(TOURNAMENT_LIST_ENDPOINT, typeRef<List<Tournament>>()) ?: emptyList()

    init {
        this.restTemplateBuilder = restTemplateBuilder
                .requestFactory { requestFactory }
                .additionalMessageConverters(jsonApiMessageConverter)
                .errorHandler(jsonApiErrorHandler)
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onLoggedOutEvent(event: LoggedOutEvent) {
        authorizedLatch = CountDownLatch(1)
        restOperations = null
    }

    @Subscribe
    fun onLoginSuccessEvent(event: LoginSuccessEvent) {
        authorize(event.userId, event.username, event.password)
    }

    @SuppressWarnings("unchecked")
    override fun getPlayerAchievements(playerId: Int): List<PlayerAchievement> {
        return getAll("/data/playerAchievement", mapOf(
                "filter" to rsql(qBuilder().intNum("player.id").eq(playerId))
        ))
    }

    @SuppressWarnings("unchecked")
    override fun getPlayerEvents(playerId: Int): List<PlayerEvent> {
        return getAll("/data/playerEvent", mapOf(
                "filter" to rsql(qBuilder().intNum("player.id").eq(playerId))
        ))
    }

    @Cacheable(CacheNames.ACHIEVEMENTS)
    override fun getAchievementDefinition(achievementId: String): AchievementDefinition? {
        return getOne("/data/achievement/$achievementId")
    }

    override fun getLadder1v1EntryForPlayer(playerId: Int): Ladder1v1LeaderboardEntry? {
        return getOne("/leaderboards/ladder1v1/$playerId")
    }

    @Cacheable(CacheNames.RATING_HISTORY)
    override fun getGamePlayerStats(playerId: Int, knownFeaturedMod: KnownFeaturedMod): List<GamePlayerStats> {
        return getAll("/data/gamePlayerStats", mapOf(
                "filter" to rsql(qBuilder()
                .intNum("player.id").eq(playerId)
                .and()
                .string("game.featuredMod.technicalName").eq(knownFeaturedMod.technicalName)
        )))
    }

    @Cacheable(CacheNames.MAPS)
    override fun getMostPlayedMaps(count: Int, page: Int): List<Map> {
        return this.getPage<MapStatistics>("/data/mapStatistics", count, page, mapOf(
                "include" to "map,map.statistics,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player",
                "sort" to "-plays")).stream()
                .map { it.map!! } /* FIXME */
        .collect(Collectors.toList())
    }

    override fun getHighestRatedMaps(count: Int, page: Int): List<Map> {
        return this.getPage<MapStatistics>("/data/mapStatistics", count, page, mapOf(
                "include" to "map.statistics,map,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player,map.latestVersion.reviewsSummary",
                "sort" to "-map.latestVersion.reviewsSummary.lowerBound")).stream()
                .map { it.map!!}    /* FIXME */
        .collect(Collectors.toList())
    }

    override fun getNewestMaps(count: Int, page: Int): List<Map> {
        return getPage(MAP_ENDPOINT, count, page, mapOf(
                "include" to "statistics,latestVersion,author,versions.reviews,versions.reviews.player",
                "sort" to "-updateTime"))
    }

    override fun getLastGamesOnMap(playerId: Int, mapVersionId: String, count: Int): List<Game> {
        return getMany("/data/game", count, mapOf(
                "filter" to rsql(qBuilder()
                .string("mapVersion.id").eq(mapVersionId)
                .and()
                .intNum("playerStats.player.id").eq(playerId)),
                "sort" to "-endTime"
        ))
    }

    override fun uploadMod(file: Path, listener: ByteCountListener) {
        val multipartContent = createFileMultipart(file, listener)
        post("/mods/upload", multipartContent, false)
    }

    override fun uploadMap(file: Path, isRanked: Boolean, listener: ByteCountListener) {
        val multipartContent = createFileMultipart(file, listener)
        multipartContent.add("metadata", mapOf("isRanked" to isRanked))
        post("/maps/upload", multipartContent, false)
    }

    override fun changePassword(username: String, currentPasswordHash: String, newPasswordHash: String) {
        val body = mapOf(
                "currentPassword" to currentPasswordHash,
                "newPassword" to newPasswordHash
        )
        post("/users/changePassword", body, true)
    }

    override fun getModVersion(uid: String): ModVersion {
        val versions: List<ModVersion> = getMany("/data/modVersion", 1,
                mapOf("filter" to rsql(qBuilder().string("uid").eq(uid)),
                      "include" to "mod,mod.latestVersion,mod.versions,mod.uploader")
        )
        return versions[0]
    }

    @Cacheable(CacheNames.FEATURED_MOD_FILES)
    override fun getFeaturedModFiles(featuredMod: FeaturedMod, version: Int): List<FeaturedModFile> {
        val endpoint = String.format("/featuredMods/%s/files/%s", featuredMod.getId(),
                Optional.ofNullable(version).map { it.toString() }.orElse("latest"))
        return getMany(endpoint, 10000, emptyMap())
    }

    override fun getNewestReplays(count: Int, page: Int): List<Game> {
        return getPage("/data/game", count, page, mapOf(
                "sort" to "-endTime",
                "include" to REPLAY_INCLUDES,
                "filter" to "endTime=isnull=false"
        ))
    }

    override fun getHighestRatedReplays(count: Int, page: Int): List<Game> {
        return getPage("/data/game", count, page, mapOf(
                "sort" to "-reviewsSummary.lowerBound",
                "include" to REPLAY_INCLUDES,
                "filter" to "endTime=isnull=false"
        ))
    }

    override fun findReplaysByQuery(query: String, maxResults: Int, page: Int, sortConfig: SortConfig): List<Game> {
        return getPage("/data/game", maxResults, page, mapOf(
                "filter" to "($query);endTime=isnull=false",
                "include" to REPLAY_INCLUDES,
                "sort" to sortConfig.toQuery()
        ))
    }

    override fun findMapByFolderName(folderName: String): Optional<MapVersion> {
        val maps: List<MapVersion> = getMany("/data/mapVersion", 1, mapOf(
                "filter" to String.format("filename==\"*%s*\"", folderName),
                "include" to "map,map.statistics,reviews"))
        return if (maps.isEmpty()) {
            Optional.empty()
        } else Optional.ofNullable(maps[0])
    }

    override fun getPlayersByIds(playerIds: Collection<Int>): List<Player> {
        val ids = playerIds.stream().map { it.toString() }.collect(Collectors.toList())

        return getMany("/data/player", playerIds.size, mapOf(
                "include" to PLAYER_INCLUDES,
                "filter" to rsql(qBuilder().string("id").`in`(ids))
        ))
    }

    override fun createGameReview(review: GameReview): GameReview? {
        return post("/data/game/" + review.game.id + "/reviews", review, GameReview::class.java)
    }

    override fun updateGameReview(review: GameReview) {
        patch("/data/gameReview/" + review.id, review, Void::class.java)
    }

    override fun createModVersionReview(review: ModVersionReview): ModVersionReview? {
        return post("/data/modVersion/" + review.modVersion.id + "/reviews", review, ModVersionReview::class.java)
    }

    override fun updateModVersionReview(review: ModVersionReview) {
        patch("/data/modVersionReview/" + review.id, review, Void::class.java)
    }

    override fun createMapVersionReview(review: MapVersionReview): MapVersionReview? {
        return post("/data/mapVersion/" + review.mapVersion.id + "/reviews", review, MapVersionReview::class.java)
    }

    override fun updateMapVersionReview(review: MapVersionReview) {
        patch("/data/mapVersionReview/" + review.id, review, Void::class.java)
    }

    override fun deleteGameReview(id: String) {
        delete("/data/gameReview/$id")
    }

    override fun deleteMapVersionReview(id: String) {
        delete("/data/mapVersionReview/$id")
    }

    override fun findModsByQuery(query: SearchConfig, page: Int, maxResults: Int): List<Mod> {
        val parameterMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        if (query.hasQuery()) {
            parameterMap.add("filter", query.searchQuery + ";latestVersion.hidden==\"false\"")
        }
        parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,latestVersion.reviewsSummary")
        parameterMap.add("sort", query.sortConfig.toQuery())
        return getPage(MOD_ENDPOINT, maxResults, page, parameterMap)
    }

    override fun deleteModVersionReview(id: String) {
        delete("/data/modVersionReview/$id")
    }

    override fun findReplayById(id: Int): Optional<Game> {
        return Optional.ofNullable(getOne("/data/game/$id", mapOf("include" to REPLAY_INCLUDES)))
    }

    override fun getLadder1v1Maps(count: Int, page: Int): List<Ladder1v1Map> {
        return getPage("/data/ladder1v1Map", count, page, mapOf(
                "include" to "mapVersion,mapVersion.map,mapVersion.map.latestVersion,mapVersion.map.latestVersion.reviews,mapVersion.map.author,mapVersion.map.statistics"))
    }

    override fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): List<MapVersion> {
        return getPage("/data/mapVersion", loadMoreCount, page, mapOf(
                "include" to "map,map.latestVersion,map.latestVersion.reviews,map.author,map.statistics",
                "filter" to rsql(qBuilder().string("map.author.id").eq(playerId.toString()))
        ))
    }

    override fun updateMapVersion(id: String, mapVersion: MapVersion) {
        patch(String.format("/data/mapVersion/%s", id), mapVersion, Void::class.java)
    }

    @Cacheable(CacheNames.CLAN)
    override fun getClanByTag(tag: String): Optional<Clan> {
        val clans: List<Clan> = getMany("/data/clan", 1, mapOf(
                "include" to "leader,founder,memberships,memberships.player",
                "filter" to rsql(qBuilder().string("tag").eq(tag))
        ))
        return if (clans.isEmpty()) {
            Optional.empty()
        } else Optional.ofNullable(clans[0])
    }

    override fun findMapsByQuery(searchConfig: SearchConfig, page: Int, count: Int): List<Map> {
        val parameterMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        if (searchConfig.hasQuery()) {
            parameterMap.add("filter", searchConfig.searchQuery + ";latestVersion.hidden==\"false\"")
        }
        parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,author,statistics,latestVersion.reviewsSummary")
        parameterMap.add("sort", searchConfig.sortConfig.toQuery())
        return getPage(MAP_ENDPOINT, count, page, parameterMap)
    }

    override fun findMapVersionById(id: String): Optional<MapVersion> {
        // FIXME: that is not gonna work this way
        return Optional.ofNullable(getOne("$MAP_ENDPOINT/$id"))
    }

    @Cacheable(CacheNames.COOP_LEADERBOARD)
    override fun getCoopLeaderboard(missionId: String, numberOfPlayers: Int): List<CoopResult> {
        return getMany("/data/coopResult", 1000, mapOf(
                "filter" to rsql(qBuilder().intNum("playerCount").eq(numberOfPlayers)
                .and().string("mission").eq(missionId)),
                "include" to COOP_RESULT_INCLUDES,
                "sort" to "duration"
        ))
    }

    @SneakyThrows
    override fun authorize(playerId: Int, username: String, password: String) {
        val apiProperties = clientProperties.api

        val details = ResourceOwnerPasswordResourceDetails()
        details.clientId = apiProperties.clientId
        details.clientSecret = apiProperties.clientSecret
        details.clientAuthenticationScheme = AuthenticationScheme.header
        details.accessTokenUri = apiProperties.baseUrl + OAUTH_TOKEN_PATH
        details.username = username
        details.password = password

        restOperations = restTemplateBuilder
                // Base URL can be changed in login window
                .rootUri(apiProperties.baseUrl)
                .configure(OAuth2RestTemplate(details))

        authorizedLatch.countDown()
    }

    private fun createFileMultipart(file: Path, listener: ByteCountListener): MultiValueMap<String, Any> {
        val form: MultiValueMap<String, Any> = LinkedMultiValueMap()
        form.add("file", CountingFileSystemResource(file, listener))
        return form
    }

    @SneakyThrows
    private fun post(endpointPath: String, request: Any, bufferRequestBody: Boolean) {
        authorizedLatch.await()
        requestFactory.setBufferRequestBody(bufferRequestBody)

        try {
            // Don't use Void.class here, otherwise Spring won't even try to deserialize error messages in the body
            restOperations!!.postForEntity(endpointPath, request, String::class.java)
        } finally {
            requestFactory.setBufferRequestBody(true)
        }
    }

    @SneakyThrows
    private fun <T> post(endpointPath: String, request: Any, type: Class<T>): T? {
        authorizedLatch.await()
        val entity = restOperations!!.postForEntity(endpointPath, request, type)
        return entity.body
    }

    @SneakyThrows
    private fun <T> patch(endpointPath: String, request: Any, type: Class<T>): T? {
        authorizedLatch.await()
        return restOperations!!.patchForObject(endpointPath, request, type)
    }

    private fun delete(endpointPath: String) {
        restOperations!!.delete(endpointPath)
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private inline fun <reified T: Any> getOne(endpointPath: String): T? {
        return restOperations!!.getForObject(endpointPath, typeRef<T>())
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private inline fun <reified T: Any> getOne(endpointPath: String, params: kotlin.collections.Map<String, Serializable>): T? {
        val multiValues = mapToMultiMap(params.mapValues { it.toString() })
        val uriComponents = UriComponentsBuilder.fromPath(endpointPath)
                .queryParams(multiValues)
                .build()
        authorizedLatch.await()
        return getOne(uriComponents.toUriString())
    }

    private fun <T> getAll(endpointPath: String, params: kotlin.collections.Map<String, Serializable> = emptyMap()): List<T> {
        return getMany(endpointPath, clientProperties.api.maxPageSize, params)
    }

    @SneakyThrows
    private fun <T> getMany(endpointPath: String, count: Int, params: kotlin.collections.Map<String, Serializable>): List<T> {
        val result = LinkedList<T>()
        var current: List<T>? = null
        var page = 1
        val maxPageSize = clientProperties.api.maxPageSize
        while ((current == null || current.size>= maxPageSize) && result.size < count) {
            current = getPage(endpointPath, count, page++, params)
            result.addAll(current)
        }
        return result
    }

    private fun <T> getPage(endpointPath: String, pageSize: Int, page: Int, params: kotlin.collections.Map<String, Serializable>): List<T> {
        val multiValues = mapToMultiMap(params.mapValues { it.toString() })
        return getPage(endpointPath, pageSize, page, multiValues)
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private fun <T> getPage(endpointPath: String, pageSize: Int, page: Int, params: MultiValueMap<String, String>): List<T> {
        val uriComponents = UriComponentsBuilder.fromPath(endpointPath)
                .queryParams(params)
                .replaceQueryParam("page[size]", pageSize)
                .replaceQueryParam("page[number]", page)
                .build()

        authorizedLatch.await()
        return restOperations!!.getForObject(uriComponents.toUriString(), typeRef<List<T>>()) ?: emptyList()
    }

    private class qBuilder : QBuilder<qBuilder>(){}

    companion object {

        private const val MAP_ENDPOINT = "/data/map"
        private const val TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json"
        private const val REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,reviews,reviews.player,mapVersion,mapVersion.map,mapVersion.reviews,reviewsSummary"
        private const val COOP_RESULT_INCLUDES = "game.playerStats.player"
        private const val PLAYER_INCLUDES = "globalRating,ladder1v1Rating,names"
        private const val MOD_ENDPOINT = "/data/mod"
        private const val OAUTH_TOKEN_PATH = "/oauth/token"

        private fun rsql(eq: Condition<*>): String {
            return eq.query(RSQLVisitor())
        }

    }
}
