package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("featuredModFile")
class FeaturedModFile {
    @Id
    private val id: String? = null
    private val version: String? = null
    private val group: String? = null
    private val name: String? = null
    private val md5: String? = null
    private val url: String? = null
}
