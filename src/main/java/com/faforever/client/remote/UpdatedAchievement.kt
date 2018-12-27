package com.faforever.client.remote

import com.faforever.client.api.dto.AchievementState

class UpdatedAchievement (
        val achievementId: String,
        val currentState: AchievementState,
        val currentSteps: Int,
        val newlyUnlocked: Boolean
)
