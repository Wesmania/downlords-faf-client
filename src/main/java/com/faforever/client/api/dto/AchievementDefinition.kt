package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("achievement")
class AchievementDefinition {

    @Id
    private val id: String? = null
    private val description: String? = null
    private val experiencePoints: Int = 0
    private val initialState: AchievementState? = null
    private val name: String? = null
    private val revealedIconUrl: String? = null
    private val totalSteps: Integer? = null
    private val type: AchievementType? = null
    private val unlockedIconUrl: String? = null
    private val order: Int = 0
}
