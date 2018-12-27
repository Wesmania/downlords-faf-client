package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.AllArgsConstructor
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mod")
@AllArgsConstructor
@NoArgsConstructor
class Mod {

    @Id
    private val id: String? = null
    private val displayName: String? = null
    private val author: String? = null
    private val createTime: OffsetDateTime? = null
    private val updateTime: OffsetDateTime? = null

    @Relationship("uploader")
    private val uploader: Player? = null

    @Relationship("versions")
    private val versions: List<ModVersion>? = null

    @Relationship("latestVersion")
    private val latestVersion: ModVersion? = null
}
