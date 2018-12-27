package com.faforever.client.tournament

import com.faforever.client.api.dto.Tournament
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.time.OffsetDateTime

class TournamentBean {
    private val id: StringProperty
    private val name: StringProperty
    private val description: StringProperty
    private val tournamentType: StringProperty
    private val createdAt: ObjectProperty<OffsetDateTime>
    private val participantCount: IntegerProperty
    private val startingAt: ObjectProperty<OffsetDateTime>
    private val completedAt: ObjectProperty<OffsetDateTime>
    private val challongeUrl: StringProperty
    private val liveImageUrl: StringProperty
    private val signUpUrl: StringProperty
    private val openForSignup: BooleanProperty

    var isOpenForSignup: Boolean
        get() = openForSignup.get()
        set(openForSignup) = this.openForSignup.set(openForSignup)

    init {
        id = SimpleStringProperty()
        name = SimpleStringProperty()
        description = SimpleStringProperty()
        tournamentType = SimpleStringProperty()
        createdAt = SimpleObjectProperty()
        participantCount = SimpleIntegerProperty()
        startingAt = SimpleObjectProperty()
        completedAt = SimpleObjectProperty()
        challongeUrl = SimpleStringProperty()
        liveImageUrl = SimpleStringProperty()
        signUpUrl = SimpleStringProperty()
        openForSignup = SimpleBooleanProperty()
    }

    fun getId(): String {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun getName(): String {
        return name.get()
    }

    fun setName(name: String) {
        this.name.set(name)
    }

    fun nameProperty(): StringProperty {
        return name
    }

    fun getDescription(): String {
        return description.get()
    }

    fun setDescription(description: String) {
        this.description.set(description)
    }

    fun descriptionProperty(): StringProperty {
        return description
    }

    fun getTournamentType(): String {
        return tournamentType.get()
    }

    fun setTournamentType(tournamentType: String) {
        this.tournamentType.set(tournamentType)
    }

    fun tournamentTypeProperty(): StringProperty {
        return tournamentType
    }

    fun getCreatedAt(): OffsetDateTime {
        return createdAt.get()
    }

    fun setCreatedAt(createdAt: OffsetDateTime) {
        this.createdAt.set(createdAt)
    }

    fun createdAtProperty(): ObjectProperty<OffsetDateTime> {
        return createdAt
    }

    fun getParticipantCount(): Int {
        return participantCount.get()
    }

    fun setParticipantCount(participantCount: Int) {
        this.participantCount.set(participantCount)
    }

    fun participantCountProperty(): IntegerProperty {
        return participantCount
    }

    fun getStartingAt(): OffsetDateTime {
        return startingAt.get()
    }

    fun setStartingAt(startingAt: OffsetDateTime) {
        this.startingAt.set(startingAt)
    }

    fun startingAtProperty(): ObjectProperty<OffsetDateTime> {
        return startingAt
    }

    fun getCompletedAt(): OffsetDateTime {
        return completedAt.get()
    }

    fun setCompletedAt(completedAt: OffsetDateTime) {
        this.completedAt.set(completedAt)
    }

    fun completedAtProperty(): ObjectProperty<OffsetDateTime> {
        return completedAt
    }

    fun getChallongeUrl(): String {
        return challongeUrl.get()
    }

    fun setChallongeUrl(url: String) {
        this.challongeUrl.set(url)
    }

    fun challongeUrlProperty(): StringProperty {
        return challongeUrl
    }

    fun getLiveImageUrl(): String {
        return liveImageUrl.get()
    }

    fun setLiveImageUrl(liveImageUrl: String) {
        this.liveImageUrl.set(liveImageUrl)
    }

    fun liveImageUrlProperty(): StringProperty {
        return liveImageUrl
    }

    fun getSignUpUrl(): String {
        return signUpUrl.get()
    }

    fun setSignUpUrl(signUpUrl: String) {
        this.signUpUrl.set(signUpUrl)
    }

    fun signUpUrlProperty(): StringProperty {
        return signUpUrl
    }

    fun openForSignupProperty(): BooleanProperty {
        return openForSignup
    }

    companion object {

        fun fromTournamentDto(tournament: Tournament): TournamentBean {
            val tournamentBean = TournamentBean()

            tournamentBean.setId(tournament.getId())
            tournamentBean.setName(tournament.getName())
            tournamentBean.setDescription(tournament.getDescription())
            tournamentBean.setTournamentType(tournament.getTournamentType())
            tournamentBean.setCreatedAt(tournament.getCreatedAt())
            tournamentBean.setParticipantCount(tournament.getParticipantCount())
            tournamentBean.setStartingAt(tournament.getStartingAt())
            tournamentBean.setCompletedAt(tournament.getCompletedAt())
            tournamentBean.setChallongeUrl(tournament.getChallongeUrl())
            tournamentBean.setLiveImageUrl(tournament.getLiveImageUrl())
            tournamentBean.setSignUpUrl(tournament.getSignUpUrl())
            tournamentBean.isOpenForSignup = tournament.isOpenForSignup()

            return tournamentBean
        }
    }
}
