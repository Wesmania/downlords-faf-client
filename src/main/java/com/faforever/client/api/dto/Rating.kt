package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship

open class Rating(@Id var id: String? = null) {
    var mean: Double = 0.toDouble()
    var deviation: Double = 0.toDouble()
    var rating: Double = 0.toDouble()

    @Relationship("player")
    var player: Player? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rating) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode() = id?.hashCode() ?: 0
}
