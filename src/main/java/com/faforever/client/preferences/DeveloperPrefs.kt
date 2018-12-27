package com.faforever.client.preferences

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class DeveloperPrefs {
    private val gameRepositoryUrl: StringProperty

    init {
        gameRepositoryUrl = SimpleStringProperty("https://github.com/FAForever/fa.git")
    }

    fun getGameRepositoryUrl(): String {
        return gameRepositoryUrl.get()
    }

    fun setGameRepositoryUrl(gameRepositoryUrl: String) {
        this.gameRepositoryUrl.set(gameRepositoryUrl)
    }

    fun gameRepositoryUrlProperty(): StringProperty {
        return gameRepositoryUrl
    }
}
