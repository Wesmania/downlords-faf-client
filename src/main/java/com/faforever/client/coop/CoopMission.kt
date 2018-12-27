package com.faforever.client.coop

import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class CoopMission {

    private val id: StringProperty
    private val name: StringProperty
    private val description: StringProperty
    private val version: IntegerProperty
    private val category: ObjectProperty<CoopCategory>
    private val downloadUrl: StringProperty
    private val thumbnailUrlSmall: StringProperty
    private val thumbnailUrlLarge: StringProperty
    private val mapFolderName: StringProperty

    init {
        id = SimpleStringProperty()
        name = SimpleStringProperty()
        description = SimpleStringProperty()
        version = SimpleIntegerProperty()
        category = SimpleObjectProperty()
        downloadUrl = SimpleStringProperty()
        thumbnailUrlSmall = SimpleStringProperty()
        thumbnailUrlLarge = SimpleStringProperty()
        mapFolderName = SimpleStringProperty()
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

    fun getName(): String {
        return name.get()
    }

    fun setName(name: String) {
        this.name.set(name)
    }

    fun nameProperty(): StringProperty {
        return name
    }

    fun getVersion(): Int {
        return version.get()
    }

    fun setVersion(version: Int) {
        this.version.set(version)
    }

    fun versionProperty(): IntegerProperty {
        return version
    }

    fun getCategory(): CoopCategory {
        return category.get()
    }

    fun setCategory(category: CoopCategory) {
        this.category.set(category)
    }

    fun categoryProperty(): ObjectProperty<CoopCategory> {
        return category
    }

    fun getDownloadUrl(): String {
        return downloadUrl.get()
    }

    fun setDownloadUrl(downloadUrl: String) {
        this.downloadUrl.set(downloadUrl)
    }

    fun downloadUrlProperty(): StringProperty {
        return downloadUrl
    }

    fun getThumbnailUrlSmall(): String {
        return thumbnailUrlSmall.get()
    }

    fun setThumbnailUrlSmall(thumbnailUrlSmall: String) {
        this.thumbnailUrlSmall.set(thumbnailUrlSmall)
    }

    fun thumbnailUrlSmallProperty(): StringProperty {
        return thumbnailUrlSmall
    }

    fun getThumbnailUrlLarge(): String {
        return thumbnailUrlLarge.get()
    }

    fun setThumbnailUrlLarge(thumbnailUrlLarge: String) {
        this.thumbnailUrlLarge.set(thumbnailUrlLarge)
    }

    fun thumbnailUrlLargeProperty(): StringProperty {
        return thumbnailUrlLarge
    }

    fun getMapFolderName(): String {
        return mapFolderName.get()
    }

    fun setMapFolderName(mapFolderName: String) {
        this.mapFolderName.set(mapFolderName)
    }

    fun mapFolderNameProperty(): StringProperty {
        return mapFolderName
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

    companion object {

        fun fromCoopInfo(mission: com.faforever.client.api.dto.CoopMission): CoopMission {
            val bean = CoopMission()
            bean.setId(mission.getId())
            bean.setDescription(mission.getDescription())
            bean.setName(mission.getName())
            bean.setVersion(mission.getVersion())
            bean.setCategory(CoopCategory.valueOf(mission.getCategory()))
            bean.setVersion(mission.getVersion())
            bean.setDownloadUrl(mission.getDownloadUrl())
            bean.setThumbnailUrlLarge(mission.getThumbnailUrlLarge())
            bean.setThumbnailUrlSmall(mission.getThumbnailUrlSmall())
            bean.setMapFolderName(mission.getFolderName())
            return bean
        }
    }
}
