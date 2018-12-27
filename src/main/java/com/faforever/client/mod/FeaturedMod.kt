package com.faforever.client.mod

import com.google.common.base.Strings
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.net.URL
import java.util.Optional

import com.github.nocatch.NoCatch.noCatch

class FeaturedMod {
    private val id: StringProperty
    private val technicalName: StringProperty
    private val displayName: StringProperty
    private val description: StringProperty
    private val bireusUrl: ObjectProperty<URL>
    private val gitUrl: StringProperty
    private val gitBranch: StringProperty
    private val visible: BooleanProperty

    var isVisible: Boolean
        get() = visible.get()
        set(visible) = this.visible.set(visible)

    init {
        id = SimpleStringProperty()
        technicalName = SimpleStringProperty()
        displayName = SimpleStringProperty()
        description = SimpleStringProperty()
        visible = SimpleBooleanProperty()
        gitUrl = SimpleStringProperty()
        gitBranch = SimpleStringProperty()
        bireusUrl = SimpleObjectProperty()
    }

    fun getTechnicalName(): String {
        return technicalName.get()
    }

    fun setTechnicalName(technicalName: String) {
        this.technicalName.set(technicalName)
    }

    fun technicalNameProperty(): StringProperty {
        return technicalName
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

    fun getDescription(): String {
        return description.get()
    }

    fun setDescription(description: String) {
        this.description.set(description)
    }

    fun descriptionProperty(): StringProperty {
        return description
    }

    fun visibleProperty(): BooleanProperty {
        return visible
    }

    fun getGitUrl(): String {
        return gitUrl.get()
    }

    fun setGitUrl(gitUrl: String) {
        this.gitUrl.set(gitUrl)
    }

    fun gitUrlProperty(): StringProperty {
        return gitUrl
    }

    fun getGitBranch(): String {
        return gitBranch.get()
    }

    fun setGitBranch(gitBranch: String) {
        this.gitBranch.set(gitBranch)
    }

    fun gitBranchProperty(): StringProperty {
        return gitBranch
    }

    fun getId(): String {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun getBireusUrl(): URL {
        return bireusUrl.get()
    }

    fun bireusUrlProperty(): ObjectProperty<URL> {
        return bireusUrl
    }

    companion object {

        val UNKNOWN = FeaturedMod()

        fun fromFeaturedMod(featuredMod: com.faforever.client.api.dto.FeaturedMod): FeaturedMod {
            val bean = FeaturedMod()
            bean.setId(featuredMod.getId())
            bean.technicalName.set(featuredMod.getTechnicalName())
            bean.displayName.set(featuredMod.getDisplayName())
            bean.description.value = featuredMod.getDescription()
            bean.visible.value = featuredMod.isVisible()
            bean.gitUrl.set(Strings.emptyToNull(featuredMod.getGitUrl()))
            bean.gitBranch.set(Strings.emptyToNull(featuredMod.getGitBranch()))
            Optional.ofNullable(featuredMod.getBireusUrl()).ifPresent({ url -> bean.bireusUrl.set(noCatch<URL> { URL(url + "/") }) })
            return bean
        }
    }
}
