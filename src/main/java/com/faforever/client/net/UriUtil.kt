package com.faforever.client.net

import java.net.URI
import java.net.URISyntaxException

class UriUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        fun fromString(string: String): URI {
            try {
                return URI(string)
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }

        }
    }
}
