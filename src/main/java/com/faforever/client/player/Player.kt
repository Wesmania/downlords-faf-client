package com.faforever.client.player

import com.faforever.client.chat.ChatChannelUser
import com.faforever.client.game.Game
import com.faforever.client.game.PlayerStatus
import com.faforever.client.remote.domain.GameStatus
import javafx.beans.binding.Bindings
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
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

import com.faforever.client.player.SocialStatus.OTHER
import com.faforever.client.util.FloatDelegate
import com.faforever.client.util.IntDelegate
import com.faforever.client.util.PropertyDelegate
import java.util.concurrent.Callable

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
class Player private constructor() {

    val idProperty: IntegerProperty = SimpleIntegerProperty()
    var id: Int by IntDelegate(idProperty)

    val usernameProperty: StringProperty = SimpleStringProperty()
    var username: String by PropertyDelegate(usernameProperty)

    val clanProperty: StringProperty = SimpleStringProperty()
    var clan: String by PropertyDelegate(clanProperty)

    val countryProperty: StringProperty = SimpleStringProperty()
    var country: String by PropertyDelegate(countryProperty)

    val avatarUrlProperty: StringProperty = SimpleStringProperty()
    var avatarUrl: String by PropertyDelegate(avatarUrlProperty)

    val avatarTooltipProperty: StringProperty = SimpleStringProperty()
    var avatarTooltip: String by PropertyDelegate(avatarTooltipProperty)

    val socialStatusProperty: ObjectProperty<SocialStatus> = SimpleObjectProperty(OTHER)
    var socialStatus: SocialStatus by PropertyDelegate(socialStatusProperty)

    val globalRatingDeviationProperty: FloatProperty = SimpleFloatProperty()
    var globalRatingDeviation: Float by FloatDelegate(globalRatingDeviationProperty)

    val globalRatingMeanProperty: FloatProperty = SimpleFloatProperty()
    var globalRatingMean: Float by FloatDelegate(globalRatingDeviationProperty)

    val leaderboardRatingDeviationProperty: FloatProperty = SimpleFloatProperty()
    var leaderboardRatingDeviation: Float by FloatDelegate(leaderboardRatingDeviationProperty)

    val leaderboardRatingMeanProperty: FloatProperty = SimpleFloatProperty()
    var leaderboardRatingMean: Float by FloatDelegate(leaderboardRatingDeviationProperty)

    val gameProperty: ObjectProperty<Game?> = SimpleObjectProperty()
    var game: Game?
        get() = gameProperty.get()
        set(game: Game?) {
            this.gameProperty.set(game)
            if (game == null) {
                status.unbind()
                status.set(PlayerStatus.IDLE)
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
                }, game.statusProperty()))
            }
        }

    val statusProperty: ObjectProperty<PlayerStatus> = SimpleObjectProperty(PlayerStatus.IDLE)
    val status: PlayerStatus
        get() = statusProperty.get()

    val numberOfGamesProperty: IntegerProperty = SimpleIntegerProperty()
    var numberOfGames: Int by IntDelegate(numberOfGamesProperty)

    val idleSinceProperty: ObjectProperty<Instant> = SimpleObjectProperty(Instant.now())
    var idleSince: Instant by PropertyDelegate(idleSinceProperty)

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
            val player = Player(dto.getLogin())
            player.id = Integer.parseInt(dto.getId())
            player.username = dto.getLogin()
            player.globalRatingMean = Optional.ofNullable(dto.getGlobalRating()).map(Function<T, Any> { getMean() }).orElse(0.0).floatValue()
            player.globalRatingDeviation = Optional.ofNullable(dto.getGlobalRating()).map(Function<T, Any> { getDeviation() }).orElse(0.0).floatValue()
            player.leaderboardRatingMean = Optional.ofNullable(dto.getLadder1v1Rating()).map(Function<T, Any> { getMean() }).orElse(0.0).floatValue()
            player.leaderboardRatingDeviation = Optional.ofNullable(dto.getLadder1v1Rating()).map(Function<T, Any> { getDeviation() }).orElse(0.0).floatValue()
            if (dto.getNames() != null) {
                player.names.addAll(dto.getNames().stream().map(???({ NameRecord.fromDto(it) })).collect(Collectors.toList<T>()))
            }
            return player
        }
    }
}
