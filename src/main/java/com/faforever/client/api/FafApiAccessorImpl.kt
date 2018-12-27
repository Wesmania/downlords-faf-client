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
import com.faforever.client.config.ClientProperties.Api
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.io.CountingFileSystemResource
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.io.ByteCountListener
import com.github.rutledgepaulv.qbuilders.builders.QBuilder
import com.github.rutledgepaulv.qbuilders.conditions.Condition
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor
import com.google.common.collect.ImmutableMap
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails
import org.springframework.security.oauth2.common.AuthenticationScheme
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.io.Serializable
import java.nio.file.Path
import java.util.Arrays
import java.util.Collections
import java.util.LinkedList
import kotlin.collections.Map.Entry
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
    private val requestFactory: HttpComponentsClientHttpRequestFactory

    private var authorizedLatch: CountDownLatch? = null
    private var restOperations: RestOperations? = null

    val achievementDefinitions: List<AchievementDefinition>
        @Override
        @SuppressWarnings("unchecked")
        @Cacheable(CacheNames.ACHIEVEMENTS)
        get() = getAll("/data/achievement", ImmutableMap.of(
                "sort", "order"
        ))

    val mods: List<Mod>
        @Override
        @Cacheable(CacheNames.MODS)
        get() = getAll("/data/mod", ImmutableMap.of(
                "include", "latestVersion,latestVersion.reviewsSummary"))

    val featuredMods: List<com.faforever.client.api.dto.FeaturedMod>
        @Override
        @Cacheable(CacheNames.FEATURED_MODS)
        get() = getMany("/data/featuredMod", 1000, ImmutableMap.of())

    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it's called manually
    val globalLeaderboard: List<GlobalLeaderboardEntry>
        @Override
        @Cacheable(CacheNames.GLOBAL_LEADERBOARD)
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() {
            authorizedLatch!!.await()
            return restOperations!!.getForObject("/leaderboards/global", List::class.java,
                    ImmutableMap.of(
                            "sort", "-rating",
                            "include", "player",
                            "fields[globalRating]", "rating,numGames",
                            "fields[player]", "login"
                    ))
        }

    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it doesn't use getAll()
    val ladder1v1Leaderboard: List<Ladder1v1LeaderboardEntry>
        @Override
        @Cacheable(CacheNames.LADDER_1V1_LEADERBOARD)
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() {
            authorizedLatch!!.await()
            return restOperations!!.getForObject("/leaderboards/ladder1v1", List::class.java,
                    ImmutableMap.of(
                            "sort", "-rating",
                            "include", "player",
                            "fields[ladder1v1Rating]", "rating,numGames,winGames",
                            "fields[player]", "login"
                    ))
        }

    val coopMissions: List<CoopMission>
        @Override
        @Cacheable(CacheNames.COOP_MAPS)
        get() = this.getAll<Object>("/data/coopMission")

    val allTournaments: List<Tournament>
        @Override
        @SneakyThrows
        @SuppressWarnings("unchecked")
        get() = Arrays.asList(restOperations!!.getForObject(TOURNAMENT_LIST_ENDPOINT, Array<Tournament>::class.java))

    init {
        authorizedLatch = CountDownLatch(1)

        requestFactory = HttpComponentsClientHttpRequestFactory()
        this.restTemplateBuilder = restTemplateBuilder
                .requestFactory({ requestFactory })
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

    @Override
    @SuppressWarnings("unchecked")
    fun getPlayerAchievements(playerId: Int): List<PlayerAchievement> {
        return getAll("/data/playerAchievement", ImmutableMap.of(
                "filter", rsql(qBuilder<QBuilder<T>>().intNum("player.id").eq(playerId))
        ))
    }

    @Override
    @SuppressWarnings("unchecked")
    fun getPlayerEvents(playerId: Int): List<PlayerEvent> {
        return getAll("/data/playerEvent", ImmutableMap.of(
                "filter", rsql(qBuilder<QBuilder<T>>().intNum("player.id").eq(playerId))
        ))
    }

    @Override
    @Cacheable(CacheNames.ACHIEVEMENTS)
    fun getAchievementDefinition(achievementId: String): AchievementDefinition {
        return getOne("/data/achievement/$achievementId", AchievementDefinition::class.java)
    }

    @Override
    fun getLadder1v1EntryForPlayer(playerId: Int): Ladder1v1LeaderboardEntry {
        return getOne("/leaderboards/ladder1v1/$playerId", Ladder1v1LeaderboardEntry::class.java)
    }

    @Override
    @Cacheable(CacheNames.RATING_HISTORY)
    fun getGamePlayerStats(playerId: Int, knownFeaturedMod: KnownFeaturedMod): List<GamePlayerStats> {
        return getAll("/data/gamePlayerStats", ImmutableMap.of(
                "filter", rsql(qBuilder<QBuilder<T>>()
                .intNum("player.id").eq(playerId)
                .and()
                .string("game.featuredMod.technicalName").eq(knownFeaturedMod.technicalName)
        )))
    }

    @Override
    @Cacheable(CacheNames.MAPS)
    fun getMostPlayedMaps(count: Int, page: Int): List<Map> {
        return this.getPage<MapStatistics>("/data/mapStatistics", count, page, ImmutableMap.of(
                "include", "map,map.statistics,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player",
                "sort", "-plays")).stream()
                .map(???({ MapStatistics.getMap() }))
        .collect(Collectors.toList())
    }

    @Override
    fun getHighestRatedMaps(count: Int, page: Int): List<Map> {
        return this.getPage<MapStatistics>("/data/mapStatistics", count, page, ImmutableMap.of(
                "include", "map.statistics,map,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player,map.latestVersion.reviewsSummary",
                "sort", "-map.latestVersion.reviewsSummary.lowerBound")).stream()
                .map(???({ MapStatistics.getMap() }))
        .collect(Collectors.toList())
    }

    @Override
    fun getNewestMaps(count: Int, page: Int): List<Map> {
        return getPage(MAP_ENDPOINT, count, page, ImmutableMap.of(
                "include", "statistics,latestVersion,author,versions.reviews,versions.reviews.player",
                "sort", "-updateTime"))
    }

    @Override
    fun getLastGamesOnMap(playerId: Int, mapVersionId: String, count: Int): List<Game> {
        return getMany("/data/game", count, ImmutableMap.of(
                "filter", rsql(qBuilder<QBuilder<T>>()
                .string("mapVersion.id").eq(mapVersionId)
                .and()
                .intNum("playerStats.player.id").eq(playerId)),
                "sort", "-endTime"
        ))
    }

    @Override
    fun uploadMod(file: Path, listener: ByteCountListener) {
        val multipartContent = createFileMultipart(file, listener)
        post("/mods/upload", multipartContent, false)
    }

    @Override
    fun uploadMap(file: Path, isRanked: Boolean, listener: ByteCountListener) {
        val multipartContent = createFileMultipart(file, listener)
        multipartContent.add("metadata", ImmutableMap.of("isRanked", isRanked))
        post("/maps/upload", multipartContent, false)
    }

    @Override
    fun changePassword(username: String, currentPasswordHash: String, newPasswordHash: String) {
        val body = ImmutableMap.of(
                "currentPassword", currentPasswordHash,
                "newPassword", newPasswordHash
        )

        post("/users/changePassword", body, true)
    }

    @Override
    fun getModVersion(uid: String): ModVersion {
        return getMany("/data/modVersion", 1,
                ImmutableMap.of("filter", rsql(qBuilder<QBuilder<T>>().string("uid").eq(uid)), "include", "mod,mod.latestVersion,mod.versions,mod.uploader")
        ).get(0) as ModVersion
    }

    @Override
    @Cacheable(CacheNames.FEATURED_MOD_FILES)
    fun getFeaturedModFiles(featuredMod: FeaturedMod, version: Integer): List<FeaturedModFile> {
        val endpoint = String.format("/featuredMods/%s/files/%s", featuredMod.getId(),
                Optional.ofNullable(version).map(???({ String.valueOf() })).orElse("latest"))
        return getMany(endpoint, 10000, ImmutableMap.of())
    }

    @Override
    fun getNewestReplays(count: Int, page: Int): List<Game> {
        return getPage("/data/game", count, page, ImmutableMap.of(
                "sort", "-endTime",
                "include", REPLAY_INCLUDES,
                "filter", "endTime=isnull=false"
        ))
    }

    @Override
    fun getHighestRatedReplays(count: Int, page: Int): List<Game> {
        return getPage("/data/game", count, page, ImmutableMap.of(
                "sort", "-reviewsSummary.lowerBound",
                "include", REPLAY_INCLUDES,
                "filter", "endTime=isnull=false"
        ))
    }

    @Override
    fun findReplaysByQuery(query: String, maxResults: Int, page: Int, sortConfig: SortConfig): List<Game> {
        return getPage("/data/game", maxResults, page, ImmutableMap.of(
                "filter", "($query);endTime=isnull=false",
                "include", REPLAY_INCLUDES,
                "sort", sortConfig.toQuery()
        ))
    }

    @Override
    fun findMapByFolderName(folderName: String): Optional<MapVersion> {
        val maps = getMany("/data/mapVersion", 1, ImmutableMap.of(
                "filter", String.format("filename==\"*%s*\"", folderName),
                "include", "map,map.statistics,reviews"))
        return if (maps.isEmpty()) {
            Optional.empty()
        } else Optional.ofNullable(maps.get(0))
    }

    @Override
    fun getPlayersByIds(playerIds: Collection<Integer>): List<Player> {
        val ids = playerIds.stream().map(???({ String.valueOf() })).collect(Collectors.toList())

        return getMany("/data/player", playerIds.size(), ImmutableMap.of(
                "include", PLAYER_INCLUDES,
                "filter", rsql(qBuilder<QBuilder<T>>().string("id").`in`(ids))
        ))
    }

    @Override
    fun createGameReview(review: GameReview): GameReview {
        return post("/data/game/" + review.getGame().getId() + "/reviews", review, GameReview::class.java)
    }

    @Override
    fun updateGameReview(review: GameReview) {
        patch("/data/gameReview/" + review.getId(), review, Void::class.java)
    }

    @Override
    fun createModVersionReview(review: ModVersionReview): ModVersionReview {
        return post("/data/modVersion/" + review.getModVersion().getId() + "/reviews", review, ModVersionReview::class.java)
    }

    @Override
    fun updateModVersionReview(review: ModVersionReview) {
        patch("/data/modVersionReview/" + review.getId(), review, Void::class.java)
    }

    @Override
    fun createMapVersionReview(review: MapVersionReview): MapVersionReview {
        return post("/data/mapVersion/" + review.getMapVersion().getId() + "/reviews", review, MapVersionReview::class.java)
    }

    @Override
    fun updateMapVersionReview(review: MapVersionReview) {
        patch("/data/mapVersionReview/" + review.getId(), review, Void::class.java)
    }

    @Override
    fun deleteGameReview(id: String) {
        delete("/data/gameReview/$id")
    }

    @Override
    fun deleteMapVersionReview(id: String) {
        delete("/data/mapVersionReview/$id")
    }

    @Override
    fun findModsByQuery(searchConfig: SearchConfig, page: Int, count: Int): List<Mod> {
        val parameterMap = LinkedMultiValueMap()
        if (searchConfig.hasQuery()) {
            parameterMap.add("filter", searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"")
        }
        parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,latestVersion.reviewsSummary")
        parameterMap.add("sort", searchConfig.getSortConfig().toQuery())
        return getPage<Object>(MOD_ENDPOINT, count, page, parameterMap)
    }

    @Override
    fun deleteModVersionReview(id: String) {
        delete("/data/modVersionReview/$id")
    }

    @Override
    fun findReplayById(id: Int): Optional<Game> {
        return Optional.ofNullable(getOne("/data/game/$id", Game::class.java, ImmutableMap.of("include", REPLAY_INCLUDES)))
    }

    @Override
    fun getLadder1v1Maps(count: Int, page: Int): List<Ladder1v1Map> {
        return getPage("/data/ladder1v1Map", count, page, ImmutableMap.of(
                "include", "mapVersion,mapVersion.map,mapVersion.map.latestVersion,mapVersion.map.latestVersion.reviews,mapVersion.map.author,mapVersion.map.statistics"))
    }

    @Override
    fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): List<MapVersion> {
        return getPage("/data/mapVersion", loadMoreCount, page, ImmutableMap.of(
                "include", "map,map.latestVersion,map.latestVersion.reviews,map.author,map.statistics",
                "filter", rsql(qBuilder<QBuilder<T>>().string("map.author.id").eq(String.valueOf(playerId)))
        ))
    }

    @Override
    fun updateMapVersion(id: String, mapVersion: MapVersion) {
        patch(String.format("/data/mapVersion/%s", id), mapVersion, Void::class.java)
    }

    @Override
    @Cacheable(CacheNames.CLAN)
    fun getClanByTag(tag: String): Optional<Clan> {
        val clans = getMany("/data/clan", 1, ImmutableMap.of(
                "include", "leader,founder,memberships,memberships.player",
                "filter", rsql(qBuilder<QBuilder<T>>().string("tag").eq(tag))
        ))
        return if (clans.isEmpty()) {
            Optional.empty()
        } else Optional.ofNullable(clans.get(0))
    }

    @Override
    fun findMapsByQuery(searchConfig: SearchConfig, page: Int, count: Int): List<Map> {
        val parameterMap = LinkedMultiValueMap()
        if (searchConfig.hasQuery()) {
            parameterMap.add("filter", searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"")
        }
        parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,author,statistics,latestVersion.reviewsSummary")
        parameterMap.add("sort", searchConfig.getSortConfig().toQuery())
        return getPage<Object>(MAP_ENDPOINT, count, page, parameterMap)
    }

    @Override
    fun findMapVersionById(id: String): Optional<MapVersion> {
        // FIXME: that is not gonna work this way
        return Optional.ofNullable(getOne("$MAP_ENDPOINT/$id", MapVersion::class.java))
    }

    @Override
    @Cacheable(CacheNames.COOP_LEADERBOARD)
    fun getCoopLeaderboard(missionId: String, numberOfPlayers: Int): List<CoopResult> {
        return getMany("/data/coopResult", 1000, ImmutableMap.of(
                "filter", rsql(qBuilder<QBuilder<T>>().intNum("playerCount").eq(numberOfPlayers)
                .and().string("mission").eq(missionId)),
                "include", COOP_RESULT_INCLUDES,
                "sort", "duration"
        ))
    }

    @Override
    @SneakyThrows
    fun authorize(playerId: Int, username: String, password: String) {
        val apiProperties = clientProperties.getApi()

        val details = ResourceOwnerPasswordResourceDetails()
        details.setClientId(apiProperties.getClientId())
        details.setClientSecret(apiProperties.getClientSecret())
        details.setClientAuthenticationScheme(AuthenticationScheme.header)
        details.setAccessTokenUri(apiProperties.getBaseUrl() + OAUTH_TOKEN_PATH)
        details.setUsername(username)
        details.setPassword(password)

        restOperations = restTemplateBuilder
                // Base URL can be changed in login window
                .rootUri(apiProperties.getBaseUrl())
                .configure(OAuth2RestTemplate(details))

        authorizedLatch!!.countDown()
    }

    @NotNull
    private fun createFileMultipart(file: Path, listener: ByteCountListener): MultiValueMap<String, Object> {
        val form = LinkedMultiValueMap()
        form.add("file", CountingFileSystemResource(file, listener))
        return form
    }

    @SneakyThrows
    private fun post(endpointPath: String, request: Object, bufferRequestBody: Boolean) {
        authorizedLatch!!.await()
        requestFactory.setBufferRequestBody(bufferRequestBody)

        try {
            // Don't use Void.class here, otherwise Spring won't even try to deserialize error messages in the body
            restOperations!!.postForEntity(endpointPath, request, String::class.java)
        } finally {
            requestFactory.setBufferRequestBody(true)
        }
    }

    @SneakyThrows
    private fun <T> post(endpointPath: String, request: Object, type: Class<T>): T {
        authorizedLatch!!.await()
        val entity = restOperations!!.postForEntity(endpointPath, request, type)
        return entity.getBody()
    }

    @SneakyThrows
    private fun <T> patch(endpointPath: String, request: Object, type: Class<T>): T {
        authorizedLatch!!.await()
        return restOperations!!.patchForObject(endpointPath, request, type)
    }

    private fun delete(endpointPath: String) {
        restOperations!!.delete(endpointPath)
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private fun <T> getOne(endpointPath: String, type: Class<T>): T {
        return restOperations!!.getForObject(endpointPath, type, Collections.emptyMap())
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private fun <T> getOne(endpointPath: String, type: Class<T>, params: java.util.Map<String, Serializable>): T {
        val multiValues = params.entrySet().stream()
                .collect(Collectors.toMap(???({ Entry.getKey() }), { entry -> Collections.singletonList(String.valueOf(entry.getValue())) }))

        val uriComponents = UriComponentsBuilder.fromPath(endpointPath)
                .queryParams(CollectionUtils.toMultiValueMap(multiValues))
                .build()

        authorizedLatch!!.await()
        return getOne(uriComponents.toUriString(), type)
    }

    private fun <T> getAll(endpointPath: String, params: java.util.Map<String, Serializable> = Collections.emptyMap()): List<T> {
        return getMany(endpointPath, clientProperties.getApi().getMaxPageSize(), params)
    }

    @SneakyThrows
    private fun <T> getMany(endpointPath: String, count: Int, params: java.util.Map<String, Serializable>): List<T> {
        val result = LinkedList()
        var current: List<T>? = null
        var page = 1
        val maxPageSize = clientProperties.getApi().getMaxPageSize()
        while ((current == null || current.size() >= maxPageSize) && result.size() < count) {
            current = getPage<Object>(endpointPath, count, page++, params)
            result.addAll(current)
        }
        return result
    }

    private fun <T> getPage(endpointPath: String, pageSize: Int, page: Int, params: java.util.Map<String, Serializable>): List<T> {
        val multiValues = params.entrySet().stream()
                .collect(Collectors.toMap(???({ Entry.getKey() }), { entry -> Collections.singletonList(String.valueOf(entry.getValue())) }))

        return getPage(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues))
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private fun <T> getPage(endpointPath: String, pageSize: Int, page: Int, params: MultiValueMap<String, String>): List<T> {
        val uriComponents = UriComponentsBuilder.fromPath(endpointPath)
                .queryParams(params)
                .replaceQueryParam("page[size]", pageSize)
                .replaceQueryParam("page[number]", page)
                .build()

        authorizedLatch!!.await()
        return restOperations!!.getForObject(uriComponents.toUriString(), List::class.java)
    }

    companion object {

        private val MAP_ENDPOINT = "/data/map"
        private val TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json"
        private val REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,reviews,reviews.player,mapVersion,mapVersion.map,mapVersion.reviews,reviewsSummary"
        private val COOP_RESULT_INCLUDES = "game.playerStats.player"
        private val PLAYER_INCLUDES = "globalRating,ladder1v1Rating,names"
        private val MOD_ENDPOINT = "/data/mod"
        private val OAUTH_TOKEN_PATH = "/oauth/token"

        private fun rsql(eq: Condition<*>): String {
            return eq.query(RSQLVisitor())
        }

        private fun <T : QBuilder<T>> qBuilder(): QBuilder<T> {
            return QBuilder()
        }
    }
}
