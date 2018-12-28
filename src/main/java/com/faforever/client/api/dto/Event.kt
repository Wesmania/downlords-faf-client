package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("event")
data class Event(@Id var id: String? = null) {
    var name: String? = null
    var imageUrl: String? = null
    var type: Type? = null

    enum class Type {
        NUMERIC, TIME
    }
}
