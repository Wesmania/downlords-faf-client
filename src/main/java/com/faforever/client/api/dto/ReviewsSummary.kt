package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@EqualsAndHashCode(of = "id")
@Setter
class ReviewsSummary {
    @Id
    private val id: String? = null
    private val positive: Float = 0.toFloat()
    private val negative: Float = 0.toFloat()
    private val score: Float = 0.toFloat()
    private val reviews: Int = 0
    private val lowerBound: Float = 0.toFloat()

}
