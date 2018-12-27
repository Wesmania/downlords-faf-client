package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Data

import java.time.OffsetDateTime

@Data
@Type("clan")
class Clan {
    @Id
    private val id: String? = null
    private val name: String? = null
    private val tag: String? = null
    private val description: String? = null
    private val tagColor: String? = null
    private val websiteUrl: String? = null
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null

    @Relationship("founder")
    private val founder: Player? = null

    @Relationship("leader")
    private val leader: Player? = null

    @Relationship("memberships")
    private val memberships: List<ClanMembership>? = null
}
