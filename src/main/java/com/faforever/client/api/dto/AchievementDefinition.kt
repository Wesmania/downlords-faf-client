package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

@Type("achievement")
data class AchievementDefinition(@Id var id: String? = null) {
    var description: String? = null
    var experiencePoints: Int = 0
    var initialState: AchievementState? = null
    var name: String? = null
    var revealedIconUrl: String? = null
    var totalSteps: Int? = null
    var type: AchievementType? = null
    var unlockedIconUrl: String? = null
    var order: Int = 0
}
