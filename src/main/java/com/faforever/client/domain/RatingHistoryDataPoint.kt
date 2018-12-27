package com.faforever.client.domain

import lombok.Data

import java.time.OffsetDateTime

@Data
class RatingHistoryDataPoint {
    private val instant: OffsetDateTime? = null
    private val mean: Float = 0.toFloat()
    private val deviation: Float = 0.toFloat()
}
