package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("clanMembership")
data class ClanMembership (
    @Id
    var id: String? = null,
    var createTime: OffsetDateTime? = null,
    var updateTime: OffsetDateTime? = null,

    @Relationship("clan")
    var clan: Clan? = null,

    @Relationship("player")
    var player: Player? = null
)