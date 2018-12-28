package com.faforever.client.player

import com.faforever.client.chat.ChatChannelUser
import com.faforever.client.game.Game
import com.faforever.client.game.PlayerStatus
import com.faforever.client.remote.domain.GameStatus
import javafx.beans.binding.Bindings
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableSet

import java.time.Instant
import java.util.Objects
import java.util.Optional
import tornadofx.getValue
import tornadofx.setValue

import com.faforever.client.player.SocialStatus.OTHER
import java.util.concurrent.Callable
import java.util.stream.Collectors.toList

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
class Player private constructor() {

    val idProperty: IntegerProperty = SimpleIntegerProperty()
    var id: Int by idProperty

    val usernameProperty: StringProperty = SimpleStringProperty()
    var username: String by usernameProperty

    val clanProperty: StringProperty = SimpleStringProperty()
    var clan: String by clanProperty

    val countryProperty: StringProperty = SimpleStringProperty()
    var country: String by countryProperty

    val avatarUrlProperty: StringProperty = SimpleStringProperty()
    var avatarUrl: String by avatarUrlProperty

    val avatarTooltipProperty: StringProperty = SimpleStringProperty()
    var avatarTooltip: String by avatarTooltipProperty

    val socialStatusProperty: ObjectProperty<SocialStatus> = SimpleObjectProperty(OTHER)
    var socialStatus: SocialStatus by socialStatusProperty

    val globalRatingDeviationProperty: FloatProperty = SimpleFloatProperty()
    var globalRatingDeviation: Float by globalRatingDeviationProperty

    val globalRatingMeanProperty: FloatProperty = SimpleFloatProperty()
    var globalRatingMean: Float by globalRatingDeviationProperty

    val leaderboardRatingDeviationProperty: FloatProperty = SimpleFloatProperty()
    var leaderboardRatingDeviation: Float by leaderboardRatingDeviationProperty

    val leaderboardRatingMeanProperty: FloatProperty = SimpleFloatProperty()
    var leaderboardRatingMean: Float by leaderboardRatingDeviationProperty

    val gameProperty: ObjectProperty<Game?> = SimpleObjectProperty()
    var game: Game?
        get() = gameProperty.get()
        set(game: Game?) {
            this.gameProperty.set(game)
            if (game == null) {
                statusProperty.unbind()
                statusProperty.value = PlayerStatus.IDLE
            } else {
                this.statusProperty.bind(Bindings.createObjectBinding(Callable<PlayerStatus> {
                    if (game.status == GameStatus.OPEN) {
                        if (game.host.equals(username, ignoreCase = true)) {
                            return@Callable PlayerStatus.HOSTING
                        }
                        return@Callable PlayerStatus.LOBBYING
                    } else if (game.status == GameStatus.CLOSED) {
                        return@Callable PlayerStatus.IDLE
                    }
                    PlayerStatus.PLAYING
                }, game.statusProperty))
            }
        }

    val statusProperty: ObjectProperty<PlayerStatus> = SimpleObjectProperty(PlayerStatus.IDLE)
    val status: PlayerStatus by statusProperty

    val numberOfGamesProperty: IntegerProperty = SimpleIntegerProperty()
    var numberOfGames: Int by numberOfGamesProperty

    val idleSinceProperty: ObjectProperty<Instant> = SimpleObjectProperty(Instant.now())
    var idleSince: Instant by idleSinceProperty

    val chatChannelUsers: ObservableSet<ChatChannelUser> = FXCollections.observableSet()
    val names: ObservableList<NameRecord> = FXCollections.observableArrayList()

    constructor(player: com.faforever.client.remote.domain.Player) : this() {

        username = player.login
        clan = player.clan
        country = player.country

        if (player.avatar != null) {
            avatarTooltip = player.avatar!!.tooltip
            avatarUrl = player.avatar!!.url
        }
    }

    constructor(username: String) : this() {
        this.username = username
    }

    override fun hashCode(): Int {
        return Objects.hash(id, username)
    }

    override fun equals(obj: Any?): Boolean {
        return (obj != null
                && obj.javaClass == Player::class.java
                && (id == (obj as Player).id && id != 0 || username.equals(obj.username, ignoreCase = true)))
    }

    fun updateFromDto(player: com.faforever.client.remote.domain.Player) {
        id = player.id
        clan = player.clan
        country = player.country

        if (player.globalRating != null) {
            globalRatingMean = player.globalRating!![0]
            globalRatingDeviation = player.globalRating!![1]
        }
        if (player.ladderRating != null) {
            leaderboardRatingMean = player.ladderRating!![0]
            leaderboardRatingDeviation = player.ladderRating!![1]
        }
        numberOfGames = player.numberOfGames
        if (player.avatar != null) {
            avatarUrl = player.avatar!!.url
            avatarTooltip = player.avatar!!.tooltip
        }
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.Player): Player {
            val player = Player(dto.login)
            player.id = Integer.parseInt(dto.id)
            player.username = dto.login
            player.globalRatingMean = Optional.ofNullable(dto.globalRating).map { it.mean }.orElse(0.0).floatValue()
            player.globalRatingDeviation = Optional.ofNullable(dto.globalRating).map { it.deviation }.orElse(0.0).floatValue()
            player.leaderboardRatingMean = Optional.ofNullable(dto.ladder1v1Rating).map { it.mean }.orElse(0.0).floatValue()
            player.leaderboardRatingDeviation = Optional.ofNullable(dto.ladder1v1Rating).map { it.deviation() }.orElse(0.0).floatValue()
            if (dto.names != null) {
                player.names.addAll(dto.names.stream().map { NameRecord.fromDto(it) }.collect(toList()))
            }
            return player
        }
    }
}
