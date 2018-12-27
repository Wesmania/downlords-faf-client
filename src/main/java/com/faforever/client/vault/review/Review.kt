package com.faforever.client.vault.review

import com.faforever.client.player.Player
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.util.Optional

class Review {
    private val id: ObjectProperty<String>
    private val text: StringProperty
    private val player: ObjectProperty<Player>
    private val score: ObjectProperty<Int>

    init {
        id = SimpleObjectProperty()
        text = SimpleStringProperty()
        score = SimpleObjectProperty()
        player = SimpleObjectProperty()
    }

    fun getId(): String {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun idProperty(): ObjectProperty<String> {
        return id
    }

    fun getText(): String {
        return text.get()
    }

    fun setText(text: String) {
        this.text.set(text)
    }

    fun textProperty(): StringProperty {
        return text
    }

    fun getPlayer(): Player {
        return player.get()
    }

    fun setPlayer(player: Player) {
        this.player.set(player)
    }

    fun playerProperty(): ObjectProperty<Player> {
        return player
    }

    fun getScore(): Int? {
        return score.get()
    }

    fun setScore(score: Int?) {
        this.score.set(score)
    }

    fun scoreProperty(): ObjectProperty<Int> {
        return score
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.Review): Review {
            val review = Review()
            review.setId(dto.getId())
            review.setText(dto.getText())
            review.setScore(Optional.ofNullable(dto.getScore()).map(Function<T, U> { it.toInt() }).orElse(0).toInt())
            review.setPlayer(Player.fromDto(dto.getPlayer()))

            if (dto.getPlayer() != null) {
                review.setPlayer(Player.fromDto(dto.getPlayer()))
            }
            return review
        }
    }
}
