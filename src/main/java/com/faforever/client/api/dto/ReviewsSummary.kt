package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id

open class ReviewsSummary(@Id var id: String? = null) {
    var positive: Float = 0.toFloat()
    var negative: Float = 0.toFloat()
    var score: Float = 0.toFloat()
    var reviews: Int = 0
    var lowerBound: Float = 0.toFloat()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReviewsSummary) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode() = id?.hashCode() ?: 0
}
