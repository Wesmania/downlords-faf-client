package com.faforever.client.preferences

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class NewsPrefs {
    private val lastReadNewsUrl: StringProperty

    init {
        lastReadNewsUrl = SimpleStringProperty()
    }

    fun getLastReadNewsUrl(): String {
        return lastReadNewsUrl.get()
    }

    fun setLastReadNewsUrl(lastReadNewsUrl: String) {
        this.lastReadNewsUrl.set(lastReadNewsUrl)
    }

    fun lastReadNewsUrlProperty(): StringProperty {
        return lastReadNewsUrl
    }
}
