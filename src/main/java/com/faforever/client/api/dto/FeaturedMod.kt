package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("featuredMod")
class FeaturedMod {
    @Id
    private val id: String? = null
    private val description: String? = null
    private val displayName: String? = null
    private val order: Int = 0
    private val gitBranch: String? = null
    private val gitUrl: String? = null
    private val bireusUrl: String? = null
    private val technicalName: String? = null
    private val visible: Boolean = false
}
