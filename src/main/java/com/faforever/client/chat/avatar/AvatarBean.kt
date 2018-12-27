package com.faforever.client.chat.avatar

import com.faforever.client.remote.domain.Avatar
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.net.URL

import com.github.nocatch.NoCatch.noCatch

class AvatarBean(url: URL?, description: String?) {
    private val url: ObjectProperty<URL>
    private val description: StringProperty

    init {
        this.url = SimpleObjectProperty(url)
        this.description = SimpleStringProperty(description)
    }

    fun getUrl(): URL? {
        return url.get()
    }

    fun setUrl(url: URL) {
        this.url.set(url)
    }

    fun urlProperty(): ObjectProperty<URL> {
        return url
    }

    fun getDescription(): String? {
        return description.get()
    }

    fun setDescription(description: String) {
        this.description.set(description)
    }

    fun descriptionProperty(): StringProperty {
        return description
    }

    companion object {

        fun fromAvatar(avatar: Avatar): AvatarBean {
            return AvatarBean(noCatch<URL> { URL(avatar.url!!) }, avatar.tooltip)
        }
    }
}
