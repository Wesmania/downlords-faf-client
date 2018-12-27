package com.faforever.client.preferences.event

import lombok.AllArgsConstructor
import lombok.Value

@Value
@AllArgsConstructor
class MissingGamePathEvent {
    private val immediateUserActionRequired: Boolean = false

    init {
        this(false)
    }
}
