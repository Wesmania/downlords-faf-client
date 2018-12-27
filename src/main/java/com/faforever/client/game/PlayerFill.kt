package com.faforever.client.game

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty

class PlayerFill(players: Int?, maxPlayers: Int?) : Comparable<PlayerFill> {

    private val players: IntegerProperty
    private val maxPlayers: IntegerProperty

    init {
        this.players = SimpleIntegerProperty(players!!)
        this.maxPlayers = SimpleIntegerProperty(maxPlayers!!)
    }

    fun playersProperty(): IntegerProperty {
        return players
    }

    fun maxPlayersProperty(): IntegerProperty {
        return maxPlayers
    }

    override fun compareTo(other: PlayerFill): Int {
        return if (getPlayers() == other.getPlayers()) {
            Integer.compare(getMaxPlayers(), other.getMaxPlayers())
        } else Integer.compare(getPlayers(), other.getPlayers())

    }

    fun getPlayers(): Int {
        return players.get()
    }

    fun setPlayers(players: Int) {
        this.players.set(players)
    }

    fun getMaxPlayers(): Int {
        return maxPlayers.get()
    }

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers.set(maxPlayers)
    }
}
