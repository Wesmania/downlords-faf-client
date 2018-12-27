package com.faforever.client.remote.domain

/**
 * A player info as received from the FAF server. The FAF server sends it as JSON string which is deserialized into an
 * instance of this class.
 */
class Player {

    var id: Int = 0
    var clan: String? = null
    var login: String? = null
    var avatar: Avatar? = null
    var country: String? = null
    var numberOfGames: Int? = null
    var globalRating: FloatArray? = null
    var ladderRating: FloatArray? = null
    var league: Map<String, String>? = null
}
