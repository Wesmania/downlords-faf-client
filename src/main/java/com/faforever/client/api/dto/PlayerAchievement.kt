package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.Instant

@Type("playerAchievement")
data class PlayerAchievement (
    @Id
    var id: String? = null,
    var state: AchievementState,
    var currentSteps: Int? = null,
    var createTime: Instant? = null,
    var updateTime: Instant? = null,

    @Relationship("achievement")
    var achievement: AchievementDefinition? = null
)