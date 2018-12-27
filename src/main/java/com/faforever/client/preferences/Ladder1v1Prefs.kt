package com.faforever.client.preferences

import com.faforever.client.game.Faction
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList

import javafx.collections.FXCollections.observableArrayList

class Ladder1v1Prefs {

    private val factions: ListProperty<Faction>

    init {
        this.factions = SimpleListProperty(observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM))
    }

    fun getFactions(): ObservableList<Faction> {
        return factions.get()
    }

    fun factionsProperty(): ListProperty<Faction> {
        return factions
    }

}
