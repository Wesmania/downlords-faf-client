package com.faforever.client.preferences

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

import java.util.Locale


class LocalizationPrefs {
    private val language: ObjectProperty<Locale>

    init {
        language = SimpleObjectProperty()
    }

    fun getLanguage(): Locale? {
        return language.get()
    }

    fun setLanguage(language: Locale) {
        this.language.set(language)
    }

    fun languageProperty(): ObjectProperty<Locale> {
        return language
    }
}
