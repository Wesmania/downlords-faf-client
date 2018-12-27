package com.faforever.client.remote

import com.faforever.client.api.dto.AchievementState

class UpdatedAchievement {

    var achievementId: String? = null
    var currentState: AchievementState? = null
    var currentSteps: Int? = null
    var newlyUnlocked: Boolean = false
}
