package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship

import java.sql.Timestamp

open class Review(@Id var id: String? = null) {
    var text: String? = null
    var score: Byte? = null
    var createTime: Timestamp? = null
    var updateTime: Timestamp? = null

    @Relationship("player")
    var player: Player? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Review) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode() = id?.hashCode() ?: 0
}
