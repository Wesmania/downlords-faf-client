package com.faforever.client.game

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.remote.domain.GameInfoMessage
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.VictoryCondition
import com.faforever.client.util.IntDelegate
import com.faforever.client.util.PropertyDelegate
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
import java.lang.RuntimeException

import java.time.Instant
import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.floor
import kotlin.math.roundToLong

class Game() {

    val idProperty: IntegerProperty = SimpleIntegerProperty()
    var id: Int by IntDelegate(idProperty)

    val hostProperty: StringProperty = SimpleStringProperty()
    var host: String by PropertyDelegate(hostProperty)

    val titleProperty: StringProperty = SimpleStringProperty()
    var title: String by PropertyDelegate(titleProperty)

    val mapFolderNameProperty: StringProperty = SimpleStringProperty()
    var mapFolderName: String by PropertyDelegate(mapFolderNameProperty)

    val featuredModProperty: StringProperty = SimpleStringProperty()
    var featuredMod: String by PropertyDelegate(featuredModProperty)

    val numPlayersProperty: IntegerProperty = SimpleIntegerProperty()
    var numPlayers: Int by IntDelegate(numPlayersProperty)

    val maxPlayersProperty: IntegerProperty = SimpleIntegerProperty()
    var maxPlayers: Int by IntDelegate(maxPlayersProperty)

    val minRatingProperty: IntegerProperty = SimpleIntegerProperty(0)
    var minRating: Int?
            get() = minRatingProperty.value
            set(v) {
                minRatingProperty.value = v
            }

    val maxRatingProperty: IntegerProperty = SimpleIntegerProperty(3000)
    var maxRating: Int?
        get() = maxRatingProperty.value
        set(v) {
            maxRatingProperty.value = v
        }

    val passwordProtectedProperty: BooleanProperty = SimpleBooleanProperty()
    val passwordProtected: Boolean by PropertyDelegate(passwordProtectedProperty)

    val passwordProperty: StringProperty = SimpleStringProperty()
    var password: String by PropertyDelegate(passwordProperty)

    val visibilityProperty: ObjectProperty<GameVisibility> = SimpleObjectProperty()
    val visibility: GameVisibility by PropertyDelegate(visibilityProperty)

    val statusProperty: ObjectProperty<GameStatus> = SimpleObjectProperty()
    var status: GameStatus by PropertyDelegate(statusProperty)

    val victoryConditionProperty: ObjectProperty<VictoryCondition> = SimpleObjectProperty()
    var victoryCondition: VictoryCondition by PropertyDelegate(victoryConditionProperty)

    val startTimeProperty: ObjectProperty<Instant> = SimpleObjectProperty()
    var startTime: Instant? by PropertyDelegate(startTimeProperty)

    /**
     * Maps a sim mod's UID to its name.
     */
    val simModsProperty: MapProperty<String, String> = SimpleMapProperty(FXCollections.observableHashMap())
    var simMods by PropertyDelegate(simModsProperty)

    /**
     * Maps team names ("1", "2", ...) to a list of player names. **Make sure to synchronize on the return
     * value.**
     */
    val teamsProperty: MapProperty<String, List<String>> = SimpleMapProperty(FXCollections.observableHashMap())
    var teams by PropertyDelegate(teamsProperty)

    /**
     * Maps an index (1,2,3,4...) to a version number. Don't ask me what this index maps to.
     */
    val featuredModVersionsProperty: MapProperty<String, Int> = SimpleMapProperty(FXCollections.observableHashMap())
    var featuredModVersions by PropertyDelegate(featuredModVersionsProperty)

    constructor(gameInfoMessage: GameInfoMessage) : this() {
        /* FIXME - add separate type for full messages to get rid of runtime checking here */
        if (gameInfoMessage.isFull())
            throw RuntimeException("Trying to construct a game from a partial message!")
        updateFromGameInfo(gameInfoMessage, true)
    }

    internal fun updateFromGameInfo(gameInfoMessage: GameInfoMessage) {
        return updateFromGameInfo(gameInfoMessage, false)
    }

    private fun updateFromGameInfo(gameInfoMessage: GameInfoMessage, isConstructing: Boolean) {
        /* Since this method synchronizes on and updates members of "game", deadlocks can happen easily (updates can fire
     events on the event bus, and each event subscriber is synchronized as well). By ensuring that we run all updates
     in the application thread, we eliminate this risk. This is not required during construction of the game however,
     since members are not yet accessible from outside. */
        if (!isConstructing) {
            JavaFxUtil.assertApplicationThread()
        }

        id = gameInfoMessage.uid ?: id
        host = gameInfoMessage.host ?: host
        if (gameInfoMessage.title != null)
            title = StringEscapeUtils.unescapeHtml4(gameInfoMessage.title)
        mapFolderName = gameInfoMessage.mapname ?: mapFolderName
        featuredMod = gameInfoMessage.featuredMod ?: featuredMod
        numPlayers = gameInfoMessage.numPlayers ?: numPlayers
        maxPlayers = gameInfoMessage.maxPlayers ?: maxPlayers
        victoryCondition = gameInfoMessage.gameType ?: victoryCondition
        status = gameInfoMessage.state ?: status
        passwordProtectedProperty.set(gameInfoMessage.passwordProtected ?: passwordProtected)
        Optional.ofNullable(gameInfoMessage.launchedAt).ifPresent {
            startTime = TimeUtil.fromPythonTime(floor(it)).toInstant()
        }
        loadSimMods(gameInfoMessage)
        loadTeams(gameInfoMessage)
        loadFeaturedModVersions(gameInfoMessage)
        parseRatingLimits()
    }

    private fun loadSimMods(gameInfoMessage: GameInfoMessage) {
        synchronized(simModsProperty.get()) {
            simMods.clear()
            if (gameInfoMessage.simMods != null) {
                simMods.putAll(gameInfoMessage.simMods)
            }
        }
    }

    private fun loadTeams(gameInfoMessage: GameInfoMessage) {
        synchronized(teamsProperty.get()) {
            teams.clear()
            if (gameInfoMessage.teams != null) {
                teams.putAll(gameInfoMessage.teams)
            }
        }
    }

    private fun loadFeaturedModVersions(gameInfoMessage: GameInfoMessage) {
        synchronized(featuredModVersionsProperty.get()) {
            featuredModVersions.clear()
            if (gameInfoMessage.featuredModVersions != null) {
                featuredModVersions.putAll(gameInfoMessage.featuredModVersions)
            }
        }
    }

    private fun parseRatingLimits() {
        // TODO this can be removed as soon as we valueOf server side support. Until then, let's be hacky
        val titleString = title

        var matcher = BETWEEN_RATING_PATTERN.matcher(titleString)
        if (matcher.find()) {
            minRating = parseRating(matcher.group(1))
            maxRating = parseRating(matcher.group(2))
            return
        }

        matcher = MIN_RATING_PATTERN.matcher(titleString)
        if (matcher.find()) {
            maxRating = null
            if (matcher.group(1) != null) {
                minRating = parseRating(matcher.group(1))
            }
            if (matcher.group(2) != null) {
                minRating = parseRating(matcher.group(2))
            }
            return
        }

        matcher = MAX_RATING_PATTERN.matcher(titleString)
        if (matcher.find()) {
            minRating = null
            maxRating = parseRating(matcher.group(1))
            return
        }

        matcher = ABOUT_RATING_PATTERN.matcher(titleString)
        if (matcher.find()) {
            val rating = parseRating(matcher.group(1))
            if (rating != null) {
                minRating = rating - 300
                maxRating = rating + 300
            } else {
                minRating = null
                maxRating = null
            }
        }
    }

    private fun parseRating(string: String): Int? {
        return try {
            Integer.parseInt(string)
        } catch (e: NumberFormatException) {
            var rating: Int
            val split = string.replace("k", "").split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                rating = Integer.parseInt(split[0]) * 1000
                if (split.size == 2) {
                    rating += Integer.parseInt(split[1]) * 100
                }
                rating
            } catch (e1: NumberFormatException) {
                null
            }
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        return obj is Game && id == obj.id
    }

    override fun toString(): String {
        return "Game{" +
                "title=" + title +
                ", id=" + id +
                ", status=" + status +
                "}"
    }

    companion object {
        private val RATING_NUMBER = "\\d+(?:\\.\\d+)?k?"
        private val MIN_RATING_PATTERN = Pattern.compile(">\\s*($RATING_NUMBER)|($RATING_NUMBER)\\s*\\+")
        private val MAX_RATING_PATTERN = Pattern.compile("<\\s*($RATING_NUMBER)")
        private val ABOUT_RATING_PATTERN = Pattern.compile("~\\s*($RATING_NUMBER)")
        private val BETWEEN_RATING_PATTERN = Pattern.compile("($RATING_NUMBER)\\s*-\\s*($RATING_NUMBER)")
    }
}
