package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("mod")
class Mod(@Id var id: String? = null) {

    var displayName: String? = null
    var author: String? = null
    var createTime: OffsetDateTime? = null
    var updateTime: OffsetDateTime? = null

    @Relationship("uploader")
    var uploader: Player? = null

    @Relationship("versions")
    var versions: List<ModVersion>? = null

    @Relationship("latestVersion")
    var latestVersion: ModVersion? = null
}
