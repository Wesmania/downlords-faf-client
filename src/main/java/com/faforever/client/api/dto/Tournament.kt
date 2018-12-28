package com.faforever.client.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeName
import com.github.jasminb.jsonapi.annotations.Id

import java.time.OffsetDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.WRAPPER_OBJECT)
@JsonTypeName("tournament")
data class Tournament(@Id var id: String? = null) {
    var name: String? = null
    var description: String? = null
    @JsonProperty("tournament_type")
    var tournamentType: String? = null
    @JsonProperty("created_at")
    var createdAt: OffsetDateTime? = null
    @JsonProperty("participants_count")
    var participantCount: Int = 0
    @JsonProperty("start_at")
    var startingAt: OffsetDateTime? = null
    @JsonProperty("completed_at")
    var completedAt: OffsetDateTime? = null
    @JsonProperty("full_challonge_url")
    var challongeUrl: String? = null
    @JsonProperty("live_image_url")
    var liveImageUrl: String? = null
    @JsonProperty("sign_up_url")
    var signUpUrl: String? = null
    @JsonProperty("open_signup")
    var openForSignup: Boolean = false
}
