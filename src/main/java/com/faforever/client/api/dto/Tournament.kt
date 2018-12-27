package com.faforever.client.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeName
import com.github.jasminb.jsonapi.annotations.Id
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.WRAPPER_OBJECT)
@JsonTypeName("tournament")
class Tournament {
    @Id
    private val id: String? = null
    private val name: String? = null
    private val description: String? = null
    @JsonProperty("tournament_type")
    private val tournamentType: String? = null
    @JsonProperty("created_at")
    private val createdAt: OffsetDateTime? = null
    @JsonProperty("participants_count")
    private val participantCount: Int = 0
    @JsonProperty("start_at")
    private val startingAt: OffsetDateTime? = null
    @JsonProperty("completed_at")
    private val completedAt: OffsetDateTime? = null
    @JsonProperty("full_challonge_url")
    private val challongeUrl: String? = null
    @JsonProperty("live_image_url")
    private val liveImageUrl: String? = null
    @JsonProperty("sign_up_url")
    private val signUpUrl: String? = null
    @JsonProperty("open_signup")
    private val openForSignup: Boolean = false
}
