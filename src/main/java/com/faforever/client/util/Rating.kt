package com.faforever.client.util

import lombok.Data

@Data
class Rating {
    private val mean: Double = 0.toDouble()
    private val deviation: Double = 0.toDouble()
}
