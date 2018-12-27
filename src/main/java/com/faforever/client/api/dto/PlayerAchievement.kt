package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Data

import java.time.Instant

@Data
@Type("playerAchievement")
class PlayerAchievement {

    @Id
    private val id: String? = null
    private val state: AchievementState? = null
    private val currentSteps: Integer? = null
    private val createTime: Instant? = null
    private val updateTime: Instant? = null

    @Relationship("achievement")
    private val achievement: AchievementDefinition? = null
}
