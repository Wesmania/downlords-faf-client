package com.faforever.client.fa.relay

/**
 * See values for description.
 */
enum class LobbyMode private constructor(val mode: Int) {

    /**
     * Default lobby where players can select their faction, teams and so on.
     */
    DEFAULT_LOBBY(0),

    /**
     * The lobby is skipped; the preferences starts straight away,
     */
    NO_LOBBY(1)
}
