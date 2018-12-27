package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.sql.Timestamp

@Getter
@Setter
@EqualsAndHashCode(of = "id")
class Review {
    @Id
    private val id: String? = null
    private val text: String? = null
    private val score: Byte? = null
    private val createTime: Timestamp? = null
    private val updateTime: Timestamp? = null

    @Relationship("player")
    private val player: Player? = null
}
