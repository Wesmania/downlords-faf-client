package com.faforever.client.replay

import com.faforever.client.api.dto.Game
import com.faforever.client.api.dto.GamePlayerStats
import com.faforever.client.api.dto.Validity
import com.faforever.client.map.MapBean
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.vault.review.Review
import javafx.beans.Observable
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import lombok.Data
import org.apache.commons.lang3.StringEscapeUtils

import java.nio.file.Path
import java.time.Duration
import java.time.temporal.Temporal
import java.util.LinkedList
import java.util.Optional
import java.util.stream.Collectors

import com.faforever.client.util.TimeUtil.fromPythonTime

class Replay() {

    private val id: IntegerProperty
    private val title: StringProperty
    private val teams: MapProperty<String, List<String>>
    private val teamPlayerStats: MapProperty<String, List<PlayerStats>>
    private val startTime: ObjectProperty<Temporal>
    private val endTime: ObjectProperty<Temporal>
    private val featuredMod: ObjectProperty<FeaturedMod>
    private val map: ObjectProperty<MapBean>
    private val replayFile: ObjectProperty<Path>
    private val views: IntegerProperty
    private val chatMessages: ListProperty<ChatMessage>
    private val gameOptions: ListProperty<GameOption>
    private val reviews: ListProperty<Review>
    private val validity: ObjectProperty<Validity>

    constructor(title: String) : this() {
        this.title.set(title)
    }

    init {
        id = SimpleIntegerProperty()
        title = SimpleStringProperty()
        teams = SimpleMapProperty(FXCollections.observableHashMap())
        teamPlayerStats = SimpleMapProperty(FXCollections.observableHashMap())
        startTime = SimpleObjectProperty()
        endTime = SimpleObjectProperty()
        featuredMod = SimpleObjectProperty()
        map = SimpleObjectProperty()
        replayFile = SimpleObjectProperty()
        views = SimpleIntegerProperty()
        chatMessages = SimpleListProperty(FXCollections.observableArrayList())
        gameOptions = SimpleListProperty(FXCollections.observableArrayList())
        reviews = SimpleListProperty(FXCollections.observableArrayList { param -> arrayOf(param.scoreProperty(), param.textProperty()) })
        validity = SimpleObjectProperty()
    }

    constructor(replayInfo: LocalReplayInfo, replayFile: Path, featuredMod: FeaturedMod, mapBean: MapBean) : this() {
        id.set(replayInfo.uid!!)
        title.set(StringEscapeUtils.unescapeHtml4(replayInfo.title))
        startTime.set(fromPythonTime(if (replayInfo.gameTime > 0) replayInfo.gameTime else replayInfo.launchedAt))
        endTime.set(fromPythonTime(replayInfo.gameEnd))
        this.featuredMod.set(featuredMod)
        map.set(mapBean)
        this.replayFile.set(replayFile)
        if (replayInfo.teams != null) {
            teams.putAll(replayInfo.teams)
        }
    }

    fun getValidity(): Validity {
        return validity.get()
    }

    fun setValidity(validity: Validity) {
        this.validity.set(validity)
    }

    fun validityProperty(): ObjectProperty<Validity> {
        return validity
    }

    fun getReplayFile(): Path {
        return replayFile.get()
    }

    fun setReplayFile(replayFile: Path) {
        this.replayFile.set(replayFile)
    }

    fun replayFileProperty(): ObjectProperty<Path> {
        return replayFile
    }

    fun getTitle(): String {
        return title.get()
    }

    fun setTitle(title: String) {
        this.title.set(title)
    }

    fun titleProperty(): StringProperty {
        return title
    }

    fun getTeams(): ObservableMap<String, List<String>> {
        return teams.get()
    }

    fun setTeams(teams: ObservableMap<String, List<String>>) {
        this.teams.set(teams)
    }

    fun teamsProperty(): MapProperty<String, List<String>> {
        return teams
    }

    fun getId(): Int {
        return id.get()
    }

    fun setId(id: Int) {
        this.id.set(id)
    }

    fun idProperty(): IntegerProperty {
        return id
    }

    fun getStartTime(): Temporal {
        return startTime.get()
    }

    fun setStartTime(startTime: Temporal) {
        this.startTime.set(startTime)
    }

    fun startTimeProperty(): ObjectProperty<Temporal> {
        return startTime
    }

    fun getEndTime(): Temporal? {
        return endTime.get()
    }

    fun setEndTime(endTime: Temporal) {
        this.endTime.set(endTime)
    }

    fun endTimeProperty(): ObjectProperty<Temporal> {
        return endTime
    }

    fun getFeaturedMod(): FeaturedMod {
        return featuredMod.get()
    }

    fun setFeaturedMod(featuredMod: FeaturedMod) {
        this.featuredMod.set(featuredMod)
    }

    fun featuredModProperty(): ObjectProperty<FeaturedMod> {
        return featuredMod
    }

    fun getMap(): MapBean? {
        return map.get()
    }

    fun setMap(map: MapBean) {
        this.map.set(map)
    }

    fun mapProperty(): ObjectProperty<MapBean> {
        return map
    }

    fun getViews(): Int {
        return views.get()
    }

    fun setViews(views: Int) {
        this.views.set(views)
    }

    fun viewsProperty(): IntegerProperty {
        return views
    }

    fun getChatMessages(): ObservableList<ChatMessage> {
        return chatMessages.get()
    }

    fun setChatMessages(chatMessages: ObservableList<ChatMessage>) {
        this.chatMessages.set(chatMessages)
    }

    fun chatMessagesProperty(): ListProperty<ChatMessage> {
        return chatMessages
    }

    fun getGameOptions(): ObservableList<GameOption> {
        return gameOptions.get()
    }

    fun setGameOptions(gameOptions: ObservableList<GameOption>) {
        this.gameOptions.set(gameOptions)
    }

    fun gameOptionsProperty(): ListProperty<GameOption> {
        return gameOptions
    }

    fun getTeamPlayerStats(): ObservableMap<String, List<PlayerStats>> {
        return teamPlayerStats.get()
    }

    fun setTeamPlayerStats(teamPlayerStats: ObservableMap<String, List<PlayerStats>>) {
        this.teamPlayerStats.set(teamPlayerStats)
    }

    fun teamPlayerStatsProperty(): MapProperty<String, List<PlayerStats>> {
        return teamPlayerStats
    }

    fun getReviews(): ObservableList<Review> {
        return reviews.get()
    }

    class ChatMessage(time: Duration, sender: String, message: String) {
        private val time: ObjectProperty<Duration>
        private val sender: StringProperty
        private val message: StringProperty

        init {
            this.time = SimpleObjectProperty(time)
            this.sender = SimpleStringProperty(sender)
            this.message = SimpleStringProperty(message)
        }

        fun getTime(): Duration {
            return time.get()
        }

        fun setTime(time: Duration) {
            this.time.set(time)
        }

        fun timeProperty(): ObjectProperty<Duration> {
            return time
        }

        fun getSender(): String {
            return sender.get()
        }

        fun setSender(sender: String) {
            this.sender.set(sender)
        }

        fun senderProperty(): StringProperty {
            return sender
        }

        fun getMessage(): String {
            return message.get()
        }

        fun setMessage(message: String) {
            this.message.set(message)
        }

        fun messageProperty(): StringProperty {
            return message
        }
    }

    class GameOption(key: String, value: Any) {
        private val key: StringProperty
        private val value: StringProperty

        init {
            this.key = SimpleStringProperty(key)
            this.value = SimpleStringProperty(value.toString())
        }

        fun getKey(): String {
            return key.get()
        }

        fun setKey(key: String) {
            this.key.set(key)
        }

        fun keyProperty(): StringProperty {
            return key
        }

        fun getValue(): String {
            return value.get()
        }

        fun setValue(value: String) {
            this.value.set(value)
        }

        fun valueProperty(): StringProperty {
            return value
        }
    }

    @Data
    class PlayerStats {
        private val playerId: Int = 0
        private val beforeMean: Double = 0.toDouble()
        private val beforeDeviation: Double = 0.toDouble()
        private val afterMean: Double? = null
        private val afterDeviation: Double? = null
        private val score: Int = 0

        companion object {

            fun fromDto(gamePlayerStats: GamePlayerStats): PlayerStats {
                return PlayerStats(
                        Integer.valueOf(gamePlayerStats.getPlayer().getId()),
                        gamePlayerStats.getBeforeMean(),
                        gamePlayerStats.getBeforeDeviation(),
                        if (gamePlayerStats.getAfterMean() == null) null else java.lang.Double.valueOf(gamePlayerStats.getAfterMean()),
                        if (gamePlayerStats.getAfterDeviation() == null) null else java.lang.Double.valueOf(gamePlayerStats.getAfterDeviation()),
                        gamePlayerStats.getScore()
                )
            }
        }
    }

    companion object {

        fun fromDto(dto: Game): Replay {
            val replay = Replay()
            replay.setId(Integer.parseInt(dto.getId()))
            replay.setFeaturedMod(FeaturedMod.fromFeaturedMod(dto.getFeaturedMod()))
            replay.setTitle(dto.getName())
            replay.setStartTime(dto.getStartTime())
            Optional.ofNullable(dto.getEndTime()).ifPresent(Consumer<T> { replay.setEndTime(it) })
            Optional.ofNullable(dto.getMapVersion()).ifPresent({ mapVersion -> replay.setMap(MapBean.fromMapVersionDto(dto.getMapVersion())) })
            //    replay.setViews(dto.getViews());
            replay.setTeams(teams(dto))
            replay.setTeamPlayerStats(teamPlayerStats(dto))
            replay.getReviews().setAll(reviews(dto))
            replay.setValidity(dto.getValidity())
            return replay
        }

        private fun reviews(dto: Game): ObservableList<Review> {
            return FXCollections.observableList(dto.getReviews().stream()
                    .map(???({ Review.fromDto(it) }))
            .collect(Collectors.toList<T>()))
        }

        private fun teams(dto: Game): ObservableMap<String, List<String>> {
            val teams = FXCollections.observableHashMap<String, List<String>>()
            dto.getPlayerStats()
                    .forEach { gamePlayerStats ->
                        (teams as java.util.Map<String, List<String>>).computeIfAbsent(
                                String.valueOf(gamePlayerStats.getTeam())
                        ) { s -> LinkedList() }.add(gamePlayerStats.getPlayer().getLogin())
                    }
            return teams
        }

        private fun teamPlayerStats(dto: Game): ObservableMap<String, List<PlayerStats>> {
            val teams = FXCollections.observableHashMap<String, List<PlayerStats>>()
            dto.getPlayerStats()
                    .forEach { gamePlayerStats ->
                        (teams as java.util.Map<String, List<PlayerStats>>).computeIfAbsent(
                                String.valueOf(gamePlayerStats.getTeam())
                        ) { s -> LinkedList() }.add(PlayerStats.fromDto(gamePlayerStats))
                    }
            return teams
        }

        fun getReplayUrl(replayId: Int, baseUrlFormat: String): String {
            return String.format(baseUrlFormat, replayId)
        }
    }
}
