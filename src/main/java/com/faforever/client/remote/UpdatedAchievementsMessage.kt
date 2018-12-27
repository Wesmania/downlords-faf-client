package com.faforever.client.remote

import com.faforever.client.remote.domain.FafServerMessage
import com.faforever.client.remote.domain.FafServerMessageType

class UpdatedAchievementsMessage(val updatedAchievements: List<UpdatedAchievement>)
    :FafServerMessage(FafServerMessageType.UPDATED_ACHIEVEMENTS)
