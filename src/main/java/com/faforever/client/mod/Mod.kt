package com.faforever.client.mod

import com.faforever.client.api.dto.Player
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

import java.time.OffsetDateTime
import java.util.stream.Collectors

class Mod {
    private val id: StringProperty
    private val displayName: StringProperty
    private val author: StringProperty
    private val createTime: ObjectProperty<OffsetDateTime>
    private val updateTime: ObjectProperty<OffsetDateTime>
    private val uploader: ObjectProperty<Player>
    val versions: ObservableList<ModVersion>
    private val latestVersion: ObjectProperty<ModVersion>

    init {
        latestVersion = SimpleObjectProperty()
        id = SimpleStringProperty()
        author = SimpleStringProperty()
        displayName = SimpleStringProperty()
        createTime = SimpleObjectProperty()
        updateTime = SimpleObjectProperty()
        uploader = SimpleObjectProperty()
        versions = FXCollections.observableArrayList()
    }

    fun getId(): String {
        return id.get()
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun getDisplayName(): String {
        return displayName.get()
    }

    fun displayNameProperty(): StringProperty {
        return displayName
    }

    fun setDisplayName(displayName: String) {
        this.displayName.set(displayName)
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

    fun getCreateTime(): OffsetDateTime {
        return createTime.get()
    }

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime.set(createTime)
    }

    fun createTimeProperty(): ObjectProperty<OffsetDateTime> {
        return createTime
    }

    fun getUpdateTime(): OffsetDateTime {
        return updateTime.get()
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime.set(updateTime)
    }

    fun updateTimeProperty(): ObjectProperty<OffsetDateTime> {
        return updateTime
    }

    fun getUploader(): Player {
        return uploader.get()
    }

    fun setUploader(uploader: Player) {
        this.uploader.set(uploader)
    }

    fun uploaderProperty(): ObjectProperty<Player> {
        return uploader
    }

    fun getLatestVersion(): ModVersion {
        return latestVersion.get()
    }

    fun setLatestVersion(latestVersion: ModVersion) {
        this.latestVersion.set(latestVersion)
    }

    fun latestVersionProperty(): ObjectProperty<ModVersion> {
        return latestVersion
    }

    fun addVersions(versions: List<ModVersion>) {
        this.versions.addAll(versions)
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.Mod): Mod {
            val mod = Mod()
            mod.setId(dto.getId())
            mod.setDisplayName(dto.getDisplayName())
            mod.setAuthor(dto.getAuthor())
            mod.setCreateTime(dto.getCreateTime())
            mod.setUpdateTime(dto.getUpdateTime())
            if (dto.getUploader() != null) {
                mod.setUploader(dto.getUploader())
            }
            mod.addVersions(dto.getVersions().stream().map({ modVersion -> ModVersion.fromDto(modVersion, mod) }).collect(Collectors.toList<T>()))
            mod.setLatestVersion(ModVersion.fromDto(dto.getLatestVersion(), mod))
            return mod
        }
    }

}
