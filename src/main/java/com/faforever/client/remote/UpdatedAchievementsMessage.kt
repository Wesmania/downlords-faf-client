package com.faforever.client.remote

import com.faforever.client.remote.domain.FafServerMessage
import com.faforever.client.remote.domain.FafServerMessageType

class UpdatedAchievementsMessage : FafServerMessage(FafServerMessageType.UPDATED_ACHIEVEMENTS) {

    var updatedAchievements: List<UpdatedAchievement>? = null
}
