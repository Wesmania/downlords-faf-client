package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
class Rating {
    @Id
    private val id: String? = null
    private val mean: Double = 0.toDouble()
    private val deviation: Double = 0.toDouble()
    private val rating: Double = 0.toDouble()

    @Relationship("player")
    private val player: Player? = null
}
