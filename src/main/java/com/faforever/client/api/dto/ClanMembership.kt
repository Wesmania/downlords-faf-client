package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Data

import java.time.OffsetDateTime

@Data
@Type("clanMembership")
class ClanMembership {
    @Id
    private val id: String? = null
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null

    @Relationship("clan")
    private val clan: Clan? = null

    @Relationship("player")
    private val player: Player? = null
}
