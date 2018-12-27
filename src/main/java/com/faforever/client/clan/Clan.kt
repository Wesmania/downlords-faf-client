package com.faforever.client.clan

import com.faforever.client.player.Player
import javafx.beans.property.ListProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

import java.time.Instant
import java.util.stream.Collectors

class Clan {

    private val id: StringProperty
    private val description: StringProperty
    private val founder: ObjectProperty<Player>
    private val leader: ObjectProperty<Player>
    private val name: StringProperty
    private val tag: StringProperty
    private val tagColor: StringProperty
    private val websiteUrl: StringProperty
    private val members: ListProperty<Player>
    private val createTime: ObjectProperty<Instant>

    init {
        id = SimpleStringProperty()
        description = SimpleStringProperty()
        founder = SimpleObjectProperty()
        leader = SimpleObjectProperty()
        name = SimpleStringProperty()
        tag = SimpleStringProperty()
        tagColor = SimpleStringProperty()
        websiteUrl = SimpleStringProperty()
        members = SimpleListProperty(FXCollections.observableArrayList())
        createTime = SimpleObjectProperty()
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

    fun getDescription(): String {
        return description.get()
    }

    fun setDescription(description: String) {
        this.description.set(description)
    }

    fun descriptionProperty(): StringProperty {
        return description
    }

    fun getFounder(): Player {
        return founder.get()
    }

    fun setFounder(founder: Player) {
        this.founder.set(founder)
    }

    fun founderProperty(): ObjectProperty<Player> {
        return founder
    }

    fun getLeader(): Player {
        return leader.get()
    }

    fun setLeader(leader: Player) {
        this.leader.set(leader)
    }

    fun leaderProperty(): ObjectProperty<Player> {
        return leader
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

    fun getTag(): String {
        return tag.get()
    }

    fun setTag(tag: String) {
        this.tag.set(tag)
    }

    fun tagProperty(): StringProperty {
        return tag
    }

    fun getTagColor(): String {
        return tagColor.get()
    }

    fun setTagColor(tagColor: String) {
        this.tagColor.set(tagColor)
    }

    fun tagColorProperty(): StringProperty {
        return tagColor
    }

    fun getCreateTime(): Instant {
        return createTime.get()
    }

    fun setCreateTime(createTime: Instant) {
        this.createTime.set(createTime)
    }

    fun createTimeProperty(): ObjectProperty<Instant> {
        return createTime
    }

    fun getMembers(): ObservableList<Player> {
        return members.get()
    }

    fun membersProperty(): ListProperty<Player> {
        return members
    }

    fun getWebsiteUrl(): String {
        return websiteUrl.get()
    }

    fun setWebsiteUrl(websiteUrl: String) {
        this.websiteUrl.set(websiteUrl)
    }

    fun websiteUrlProperty(): StringProperty {
        return websiteUrl
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.Clan): Clan {
            val clan = Clan()
            clan.setId(dto.getId())
            clan.setName(dto.getName())
            clan.setDescription(dto.getDescription())
            clan.setFounder(Player.fromDto(dto.getFounder()))
            clan.setLeader(Player.fromDto(dto.getLeader()))
            clan.setCreateTime(dto.getCreateTime().toInstant())
            clan.setTag(dto.getTag())
            clan.setTagColor(dto.getTagColor())
            clan.setWebsiteUrl(dto.getWebsiteUrl())
            clan.membersProperty().setAll(dto.getMemberships().stream()
                    .map({ membership -> Player.fromDto(membership.getPlayer()) })
                    .collect(Collectors.toList<T>()))
            return clan
        }
    }
}
