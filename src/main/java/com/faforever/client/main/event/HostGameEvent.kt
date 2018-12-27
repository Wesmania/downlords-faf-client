package com.faforever.client.main.event

import lombok.AllArgsConstructor
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
class HostGameEvent : OpenCustomGamesEvent() {
    private val mapFolderName: String? = null
}
