package com.faforever.client.game

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.remote.domain.GameInfoMessage
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.VictoryCondition
import com.faforever.client.util.TimeUtil
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import org.apache.commons.lang3.StringEscapeUtils

import java.time.Instant
import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern

class Game() {

    private val host: StringProperty
    private val title: StringProperty
    private val mapFolderName: StringProperty
    private val featuredMod: StringProperty
    private val id: IntegerProperty
    private val numPlayers: IntegerProperty
    private val maxPlayers: IntegerProperty
    private val minRating: IntegerProperty
    private val maxRating: IntegerProperty
    private val passwordProtected: BooleanProperty
    private val password: StringProperty
    private val visibility: ObjectProperty<GameVisibility>
    private val status: ObjectProperty<GameStatus>
    private val victoryCondition: ObjectProperty<VictoryCondition>
    private val startTime: ObjectProperty<Instant>
    /**
     * Maps a sim mod's UID to its name.
     */
    private val simMods: MapProperty<String, String>
    private val teams: MapProperty<String, List<String>>
    /**
     * Maps an index (1,2,3,4...) to a version number. Don't ask me what this index maps to.
     */
    private val featuredModVersions: MapProperty<String, Int>

    constructor(gameInfoMessage: GameInfoMessage) : this() {
        updateFromGameInfo(gameInfoMessage, true)
    }

    init {
        id = SimpleIntegerProperty()
        host = SimpleStringProperty()
        title = SimpleStringProperty()
        mapFolderName = SimpleStringProperty()
        featuredMod = SimpleStringProperty()
        numPlayers = SimpleIntegerProperty()
        maxPlayers = SimpleIntegerProperty()
        minRating = SimpleIntegerProperty(0)
        maxRating = SimpleIntegerProperty(3000)
        passwordProtected = SimpleBooleanProperty()
        password = SimpleStringProperty()
        victoryCondition = SimpleObjectProperty()
        visibility = SimpleObjectProperty()
        simMods = SimpleMapProperty(FXCollections.observableHashMap())
        teams = SimpleMapProperty(FXCollections.observableHashMap())
        featuredModVersions = SimpleMapProperty(FXCollections.observableHashMap())
        status = SimpleObjectProperty()
        startTime = SimpleObjectProperty()
    }

    internal fun updateFromGameInfo(gameInfoMessage: GameInfoMessage) {
        updateFromGameInfo(gameInfoMessage, false)
    }

    private fun updateFromGameInfo(gameInfoMessage: GameInfoMessage, isContructing: Boolean) {
        /* Since this method synchronizes on and updates members of "game", deadlocks can happen easily (updates can fire
     events on the event bus, and each event subscriber is synchronized as well). By ensuring that we run all updates
     in the application thread, we eliminate this risk. This is not required during construction of the game however,
     since members are not yet accessible from outside. */
        if (!isContructing) {
            JavaFxUtil.assertApplicationThread()
        }

        id.set(gameInfoMessage.getUid())
        host.set(gameInfoMessage.getHost())
        title.set(StringEscapeUtils.unescapeHtml4(gameInfoMessage.getTitle()))
        mapFolderName.set(gameInfoMessage.getMapname())
        featuredMod.set(gameInfoMessage.getFeaturedMod())
        numPlayers.setValue(gameInfoMessage.getNumPlayers())
        maxPlayers.setValue(gameInfoMessage.getMaxPlayers())
        victoryCondition.set(gameInfoMessage.getGameType())
        status.set(gameInfoMessage.getState())
        passwordProtected.set(gameInfoMessage.getPasswordProtected())
        Optional.ofNullable(gameInfoMessage.getLaunchedAt()).ifPresent({ aDouble ->
            startTime.set(
                    TimeUtil.fromPythonTime(aDouble.longValue()).toInstant()
            )
        })

        synchronized(simMods.get()) {
            simMods.clear()
            if (gameInfoMessage.getSimMods() != null) {
                simMods.putAll(gameInfoMessage.getSimMods())
            }
        }

        synchronized(teams.get()) {
            teams.clear()
            if (gameInfoMessage.getTeams() != null) {
                teams.putAll(gameInfoMessage.getTeams())
            }
        }

        synchronized(featuredModVersions.get()) {
            featuredModVersions.clear()
            if (gameInfoMessage.getFeaturedModVersions() != null) {
                featuredModVersions.putAll(gameInfoMessage.getFeaturedModVersions())
            }
        }

        // TODO this can be removed as soon as we valueOf server side support. Until then, let's be hacky
        val titleString = title.get()
        var matcher = BETWEEN_RATING_PATTERN.matcher(titleString)
        if (matcher.find()) {
            minRating.set(parseRating(matcher.group(1)))
            maxRating.set(parseRating(matcher.group(2)))
        } else {
            matcher = MIN_RATING_PATTERN.matcher(titleString)
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    minRating.set(parseRating(matcher.group(1)))
                }
                if (matcher.group(2) != null) {
                    minRating.set(parseRating(matcher.group(2)))
                }
                maxRating.set(3000)
            } else {
                matcher = MAX_RATING_PATTERN.matcher(titleString)
                if (matcher.find()) {
                    minRating.set(0)
                    maxRating.setValue(parseRating(matcher.group(1)))
                } else {
                    matcher = ABOUT_RATING_PATTERN.matcher(titleString)
                    if (matcher.find()) {
                        val rating = parseRating(matcher.group(1))
                        minRating.set(rating - 300)
                        maxRating.set(rating + 300)
                    }
                }
            }
        }
    }

    private fun parseRating(string: String): Int {
        try {
            return Integer.parseInt(string)
        } catch (e: NumberFormatException) {
            var rating: Int
            val split = string.replace("k", "").split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                rating = Integer.parseInt(split[0]) * 1000
                if (split.size == 2) {
                    rating += Integer.parseInt(split[1]) * 100
                }
                return rating
            } catch (e1: NumberFormatException) {
                return Integer.MAX_VALUE
            }

        }

    }

    fun getHost(): String {
        return host.get()
    }

    fun setHost(host: String) {
        this.host.set(host)
    }

    fun hostProperty(): StringProperty {
        return host
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

    fun getMapFolderName(): String {
        return mapFolderName.get()
    }

    fun setMapFolderName(mapFolderName: String) {
        this.mapFolderName.set(mapFolderName)
    }

    fun mapFolderNameProperty(): StringProperty {
        return mapFolderName
    }

    fun getFeaturedMod(): String {
        return featuredMod.get()
    }

    fun setFeaturedMod(featuredMod: String) {
        this.featuredMod.set(featuredMod)
    }

    fun featuredModProperty(): StringProperty {
        return featuredMod
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

    fun getNumPlayers(): Int {
        return numPlayers.get()
    }

    fun setNumPlayers(numPlayers: Int) {
        this.numPlayers.set(numPlayers)
    }

    fun numPlayersProperty(): IntegerProperty {
        return numPlayers
    }

    fun getMaxPlayers(): Int {
        return maxPlayers.get()
    }

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers.set(maxPlayers)
    }

    fun maxPlayersProperty(): IntegerProperty {
        return maxPlayers
    }

    fun getMinRating(): Int {
        return minRating.get()
    }

    fun setMinRating(minRating: Int) {
        this.minRating.set(minRating)
    }

    fun minRatingProperty(): IntegerProperty {
        return minRating
    }

    fun getMaxRating(): Int {
        return maxRating.get()
    }

    fun setMaxRating(maxRating: Int) {
        this.maxRating.set(maxRating)
    }

    fun maxRatingProperty(): IntegerProperty {
        return maxRating
    }

    fun getStatus(): GameStatus {
        return status.get()
    }

    fun setStatus(status: GameStatus) {
        this.status.set(status)
    }

    fun statusProperty(): ObjectProperty<GameStatus> {
        return status
    }

    fun getVictoryCondition(): VictoryCondition {
        return victoryCondition.get()
    }

    fun setVictoryCondition(victoryCondition: VictoryCondition) {
        this.victoryCondition.set(victoryCondition)
    }

    fun victoryConditionProperty(): ObjectProperty<VictoryCondition> {
        return victoryCondition
    }

    /**
     * Returns a map of simulation mod UIDs to the mod's name.
     */
    fun getSimMods(): ObservableMap<String, String> {
        return simMods.get()
    }

    fun setSimMods(simMods: ObservableMap<String, String>) {
        this.simMods.set(simMods)
    }

    fun simModsProperty(): MapProperty<String, String> {
        return simMods
    }

    /**
     * Maps team names ("1", "2", ...) to a list of player names. **Make sure to synchronize on the return
     * value.**
     */
    fun getTeams(): ObservableMap<String, List<String>> {
        return teams.get()
    }

    fun setTeams(teams: ObservableMap<String, List<String>>) {
        this.teams.set(teams)
    }

    fun teamsProperty(): MapProperty<String, List<String>> {
        return teams
    }

    fun getFeaturedModVersions(): ObservableMap<String, Int> {
        return featuredModVersions.get()
    }

    fun setFeaturedModVersions(featuredModVersions: ObservableMap<String, Int>) {
        this.featuredModVersions.set(featuredModVersions)
    }

    fun featuredModVersionsProperty(): MapProperty<String, Int> {
        return featuredModVersions
    }

    override fun hashCode(): Int {
        return id.value!!.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        return obj is Game && id.value == obj.id.value
    }

    fun getVisibility(): GameVisibility {
        return visibility.get()
    }

    fun visibilityProperty(): ObjectProperty<GameVisibility> {
        return visibility
    }

    fun getPasswordProtected(): Boolean {
        return passwordProtected.get()
    }

    fun passwordProtectedProperty(): BooleanProperty {
        return passwordProtected
    }

    fun getPassword(): String {
        return password.get()
    }

    fun setPassword(password: String) {
        this.password.set(password)
    }

    fun passwordProperty(): StringProperty {
        return password
    }

    fun getStartTime(): Instant {
        return startTime.get()
    }

    fun setStartTime(startTime: Instant) {
        this.startTime.set(startTime)
    }

    fun startTimeProperty(): ObjectProperty<Instant> {
        return startTime
    }

    override fun toString(): String {
        return "Game{" +
                "title=" + title.get() +
                ", id=" + id.get() +
                ", status=" + status.get() +
                '}'.toString()
    }

    companion object {

        private val RATING_NUMBER = "\\d+(?:\\.\\d+)?k?"
        private val MIN_RATING_PATTERN = Pattern.compile(">\\s*($RATING_NUMBER)|($RATING_NUMBER)\\s*\\+")
        private val MAX_RATING_PATTERN = Pattern.compile("<\\s*($RATING_NUMBER)")
        private val ABOUT_RATING_PATTERN = Pattern.compile("~\\s*($RATING_NUMBER)")
        private val BETWEEN_RATING_PATTERN = Pattern.compile("($RATING_NUMBER)\\s*-\\s*($RATING_NUMBER)")
    }
}
