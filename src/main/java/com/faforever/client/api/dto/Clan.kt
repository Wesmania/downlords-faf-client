package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("clan")
data class Clan (
        @Id
        var id: String? = null,
        var name: String? = null,
        var tag: String? = null,
        var description: String? = null,
        var tagColor: String? = null,
        var websiteUrl: String? = null,
        var createTime: OffsetDateTime? = null,
        var updateTime: OffsetDateTime? = null,
        @Relationship("founder")
        var founder: Player? = null,

        @Relationship("leader")
        var leader: Player? = null,

        @Relationship("memberships")
        var memberships: List<ClanMembership>? = null
)

