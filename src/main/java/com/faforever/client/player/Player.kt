package com.faforever.client.player

import com.faforever.client.api.dto.GlobalRating
import com.faforever.client.api.dto.Ladder1v1Rating
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
import java.util.stream.Collectors

import com.faforever.client.player.SocialStatus.OTHER

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
class Player private constructor() {

    private val id: IntegerProperty
    private val username: StringProperty
    private val clan: StringProperty
    private val country: StringProperty
    private val avatarUrl: StringProperty
    private val avatarTooltip: StringProperty
    private val socialStatus: ObjectProperty<SocialStatus>
    private val globalRatingDeviation: FloatProperty
    private val globalRatingMean: FloatProperty
    private val leaderboardRatingDeviation: FloatProperty
    private val leaderboardRatingMean: FloatProperty
    private val game: ObjectProperty<Game>
    private val status: ObjectProperty<PlayerStatus>
    val chatChannelUsers: ObservableSet<ChatChannelUser>
    private val numberOfGames: IntegerProperty
    private val idleSince: ObjectProperty<Instant>
    val names: ObservableList<NameRecord>

    constructor(player: com.faforever.client.remote.domain.Player) : this() {

        username.set(player.login)
        clan.set(player.clan)
        country.set(player.country)

        if (player.avatar != null) {
            avatarTooltip.set(player.avatar!!.tooltip)
            avatarUrl.set(player.avatar!!.url)
        }
    }

    init {
        id = SimpleIntegerProperty()
        username = SimpleStringProperty()
        clan = SimpleStringProperty()
        country = SimpleStringProperty()
        avatarUrl = SimpleStringProperty()
        avatarTooltip = SimpleStringProperty()
        globalRatingDeviation = SimpleFloatProperty()
        globalRatingMean = SimpleFloatProperty()
        leaderboardRatingDeviation = SimpleFloatProperty()
        leaderboardRatingMean = SimpleFloatProperty()
        status = SimpleObjectProperty(PlayerStatus.IDLE)
        chatChannelUsers = FXCollections.observableSet()
        game = SimpleObjectProperty()
        numberOfGames = SimpleIntegerProperty()
        socialStatus = SimpleObjectProperty(OTHER)
        idleSince = SimpleObjectProperty(Instant.now())
        names = FXCollections.observableArrayList()
    }

    constructor(username: String) : this() {
        this.username.set(username)
    }

    fun getSocialStatus(): SocialStatus {
        return socialStatus.get()
    }

    fun setSocialStatus(socialStatus: SocialStatus) {
        this.socialStatus.set(socialStatus)
    }

    fun socialStatusProperty(): ObjectProperty<SocialStatus> {
        return socialStatus
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

    fun getNumberOfGames(): Int {
        return numberOfGames.get()
    }

    fun setNumberOfGames(numberOfGames: Int) {
        this.numberOfGames.set(numberOfGames)
    }

    fun numberOfGamesProperty(): IntegerProperty {
        return numberOfGames
    }

    override fun hashCode(): Int {
        return Objects.hash(id.get(), username.get())
    }

    override fun equals(obj: Any?): Boolean {
        return (obj != null
                && obj.javaClass == Player::class.java
                && (getId() == (obj as Player).getId() && getId() != 0 || getUsername().equals(obj.getUsername(), ignoreCase = true)))
    }

    fun getUsername(): String {
        return username.get()
    }

    fun setUsername(username: String) {
        this.username.set(username)
    }

    fun usernameProperty(): StringProperty {
        return username
    }

    fun getClan(): String {
        return clan.get()
    }

    fun setClan(clan: String?) {
        this.clan.set(clan)
    }

    fun clanProperty(): StringProperty {
        return clan
    }

    fun getCountry(): String {
        return country.get()
    }

    fun setCountry(country: String?) {
        this.country.set(country)
    }

    fun countryProperty(): StringProperty {
        return country
    }

    fun getAvatarUrl(): String {
        return avatarUrl.get()
    }

    fun setAvatarUrl(avatarUrl: String?) {
        this.avatarUrl.set(avatarUrl)
    }

    fun avatarUrlProperty(): StringProperty {
        return avatarUrl
    }

    fun getAvatarTooltip(): String {
        return avatarTooltip.get()
    }

    fun setAvatarTooltip(avatarTooltip: String?) {
        this.avatarTooltip.set(avatarTooltip)
    }

    fun avatarTooltipProperty(): StringProperty {
        return avatarTooltip
    }

    fun getGlobalRatingDeviation(): Float {
        return globalRatingDeviation.get()
    }

    fun setGlobalRatingDeviation(globalRatingDeviation: Float) {
        this.globalRatingDeviation.set(globalRatingDeviation)
    }

    fun globalRatingDeviationProperty(): FloatProperty {
        return globalRatingDeviation
    }

    fun getGlobalRatingMean(): Float {
        return globalRatingMean.get()
    }

    fun setGlobalRatingMean(globalRatingMean: Float) {
        this.globalRatingMean.set(globalRatingMean)
    }

    fun globalRatingMeanProperty(): FloatProperty {
        return globalRatingMean
    }

    fun getStatus(): PlayerStatus {
        return status.get()
    }

    fun statusProperty(): ReadOnlyObjectProperty<PlayerStatus> {
        return status
    }

    fun getGame(): Game {
        return game.get()
    }

    fun setGame(game: Game?) {
        this.game.set(game)
        if (game == null) {
            status.unbind()
            status.set(PlayerStatus.IDLE)
        } else {
            this.status.bind(Bindings.createObjectBinding({
                if (getGame().status == GameStatus.OPEN) {
                    if (getGame().host.equals(username.get(), ignoreCase = true)) {
                        return@Bindings.createObjectBinding PlayerStatus . HOSTING
                    }
                    return@Bindings.createObjectBinding PlayerStatus . LOBBYING
                } else if (getGame().status == GameStatus.CLOSED) {
                    return@Bindings.createObjectBinding PlayerStatus . IDLE
                }
                PlayerStatus.PLAYING
            }, game.statusProperty()))
        }
    }

    fun gameProperty(): ObjectProperty<Game> {
        return game
    }

    fun getLeaderboardRatingMean(): Float {
        return leaderboardRatingMean.get()
    }

    fun setLeaderboardRatingMean(leaderboardRatingMean: Float) {
        this.leaderboardRatingMean.set(leaderboardRatingMean)
    }

    fun leaderboardRatingMeanProperty(): FloatProperty {
        return leaderboardRatingMean
    }

    fun getLeaderboardRatingDeviation(): Float {
        return leaderboardRatingDeviation.get()
    }

    fun setLeaderboardRatingDeviation(leaderboardRatingDeviation: Float) {
        this.leaderboardRatingDeviation.set(leaderboardRatingDeviation)
    }

    fun getIdleSince(): Instant {
        return idleSince.get()
    }

    fun setIdleSince(idleSince: Instant) {
        this.idleSince.set(idleSince)
    }

    fun idleSinceProperty(): ObjectProperty<Instant> {
        return idleSince
    }

    fun leaderboardRatingDeviationProperty(): FloatProperty {
        return leaderboardRatingDeviation
    }

    fun updateFromDto(player: com.faforever.client.remote.domain.Player) {
        setId(player.id)
        setClan(player.clan)
        setCountry(player.country)

        if (player.globalRating != null) {
            setGlobalRatingMean(player.globalRating!![0])
            setGlobalRatingDeviation(player.globalRating!![1])
        }
        if (player.ladderRating != null) {
            setLeaderboardRatingMean(player.ladderRating!![0])
            setLeaderboardRatingDeviation(player.ladderRating!![1])
        }
        setNumberOfGames(player.numberOfGames!!)
        if (player.avatar != null) {
            setAvatarUrl(player.avatar!!.url)
            setAvatarTooltip(player.avatar!!.tooltip)
        }
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.Player): Player {
            val player = Player(dto.getLogin())
            player.setId(Integer.parseInt(dto.getId()))
            player.setUsername(dto.getLogin())
            player.setGlobalRatingMean(Optional.ofNullable(dto.getGlobalRating()).map(Function<T, Any> { getMean() }).orElse(0.0).floatValue())
            player.setGlobalRatingDeviation(Optional.ofNullable(dto.getGlobalRating()).map(Function<T, Any> { getDeviation() }).orElse(0.0).floatValue())
            player.setLeaderboardRatingMean(Optional.ofNullable(dto.getLadder1v1Rating()).map(Function<T, Any> { getMean() }).orElse(0.0).floatValue())
            player.setLeaderboardRatingDeviation(Optional.ofNullable(dto.getLadder1v1Rating()).map(Function<T, Any> { getDeviation() }).orElse(0.0).floatValue())
            if (dto.getNames() != null) {
                player.names.addAll(dto.getNames().stream().map(???({ NameRecord.fromDto(it) })).collect(Collectors.toList<T>()))
            }
            return player
        }
    }
}
