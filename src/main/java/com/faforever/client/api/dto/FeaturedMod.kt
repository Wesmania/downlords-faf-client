package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("featuredMod")
data class FeaturedMod(@Id var id: String? = null) {
    var description: String? = null
    var displayName: String? = null
    var order: Int = 0
    var gitBranch: String? = null
    var gitUrl: String? = null
    var bireusUrl: String? = null
    var technicalName: String? = null
    var visible: Boolean = false
}
