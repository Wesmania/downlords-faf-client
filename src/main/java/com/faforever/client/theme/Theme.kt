package com.faforever.client.theme

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.util.Objects
import java.util.Properties

open class Theme @JvmOverloads constructor(displayName: String? = null, author: String? = null, compatibilityVersion: Int? = null, themeVersion: String? = null) {

    private val displayName: StringProperty
    private val author: StringProperty
    private val compatibilityVersion: SimpleObjectProperty<Int>
    private val themeVersion: StringProperty

    init {
        this.displayName = SimpleStringProperty(displayName)
        this.author = SimpleStringProperty(author)
        this.compatibilityVersion = SimpleObjectProperty<Int>(compatibilityVersion)
        this.themeVersion = SimpleStringProperty(themeVersion)
    }

    fun getDisplayName(): String {
        return displayName.get()
    }

    fun setDisplayName(displayName: String) {
        this.displayName.set(displayName)
    }

    fun displayNameProperty(): StringProperty {
        return displayName
    }

    fun getAuthor(): String {
        return author.get()
    }

    fun setAuthor(author: String) {
        this.author.set(author)
    }

    fun authorProperty(): StringProperty {
        return author
    }

    fun getCompatibilityVersion(): Int {
        return compatibilityVersion.get()
    }

    fun setCompatibilityVersion(compatibilityVersion: Int) {
        this.compatibilityVersion.set(compatibilityVersion)
    }

    fun compatibilityVersionProperty(): SimpleObjectProperty<Int> {
        return compatibilityVersion
    }

    fun getThemeVersion(): String {
        return themeVersion.get()
    }

    fun setThemeVersion(themeVersion: String) {
        this.themeVersion.set(themeVersion)
    }

    fun themeVersionProperty(): StringProperty {
        return themeVersion
    }

    override fun hashCode(): Int {
        return Objects.hash(displayName, themeVersion)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val theme = o as Theme?
        return displayName == theme!!.displayName && themeVersion == theme.themeVersion
    }

    fun toProperties(): Properties {
        val properties = Properties()
        properties[DISPLAY_NAME] = displayName.get()
        properties[AUTHOR] = author.get()
        properties[COMPATIBILITY_VERSION] = compatibilityVersion.get()
        properties[THEME_VERSION] = themeVersion.get()
        return properties
    }

    companion object {

        private val DISPLAY_NAME = "displayName"
        private val AUTHOR = "author"
        private val COMPATIBILITY_VERSION = "compatibilityVersion"
        private val THEME_VERSION = "themeVersion"

        fun fromProperties(properties: Properties): Theme {
            return Theme(
                    properties.getProperty(DISPLAY_NAME),
                    properties.getProperty(AUTHOR),
                    Integer.valueOf(properties.getProperty(COMPATIBILITY_VERSION)),
                    properties.getProperty(THEME_VERSION)
            )
        }
    }
}
