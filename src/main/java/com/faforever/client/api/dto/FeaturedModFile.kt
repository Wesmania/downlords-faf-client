package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("featuredModFile")
data class FeaturedModFile(@Id var id: String? = null) {
    var version: String? = null
    var group: String? = null
    var name: String? = null
    var md5: String? = null
    var url: String? = null
}
