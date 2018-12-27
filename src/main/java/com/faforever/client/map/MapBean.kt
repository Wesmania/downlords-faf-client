package com.faforever.client.map

import com.faforever.client.api.dto.MapVersion
import com.faforever.client.vault.review.Review
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.maven.artifact.versioning.ComparableVersion

import java.net.URL
import java.time.LocalDateTime
import java.util.HashMap
import java.util.Optional
import java.util.stream.Collectors

class MapBean : Comparable<MapBean> {

    private val folderName: StringProperty
    private val displayName: StringProperty
    private val numberOfPlays: IntegerProperty
    private val description: StringProperty
    private val downloads: IntegerProperty
    private val players: IntegerProperty
    private val size: ObjectProperty<MapSize>
    private val version: ObjectProperty<ComparableVersion>
    private val id: StringProperty
    private val author: StringProperty
    private val hidden: BooleanProperty
    private val ranked: BooleanProperty
    private val downloadUrl: ObjectProperty<URL>
    private val smallThumbnailUrl: ObjectProperty<URL>
    private val largeThumbnailUrl: ObjectProperty<URL>
    private val createTime: ObjectProperty<LocalDateTime>
    private val type: ObjectProperty<Type>
    private val reviews: ListProperty<Review>

    var isHidden: Boolean
        get() = hidden.get()
        set(hidden) = this.hidden.set(hidden)

    var isRanked: Boolean
        get() = ranked.get()
        set(ranked) = this.ranked.set(ranked)

    init {
        id = SimpleStringProperty()
        displayName = SimpleStringProperty()
        folderName = SimpleStringProperty()
        description = SimpleStringProperty()
        numberOfPlays = SimpleIntegerProperty()
        downloads = SimpleIntegerProperty()
        players = SimpleIntegerProperty()
        size = SimpleObjectProperty()
        version = SimpleObjectProperty()
        smallThumbnailUrl = SimpleObjectProperty()
        largeThumbnailUrl = SimpleObjectProperty()
        downloadUrl = SimpleObjectProperty()
        author = SimpleStringProperty()
        createTime = SimpleObjectProperty()
        type = SimpleObjectProperty()
        reviews = SimpleListProperty(FXCollections.observableArrayList { param -> arrayOf(param.scoreProperty(), param.textProperty()) })
        hidden = SimpleBooleanProperty()
        ranked = SimpleBooleanProperty()
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

    fun getDownloadUrl(): URL {
        return downloadUrl.get()
    }

    fun setDownloadUrl(downloadUrl: URL) {
        this.downloadUrl.set(downloadUrl)
    }

    fun downloadUrlProperty(): ObjectProperty<URL> {
        return downloadUrl
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

    fun getNumberOfPlays(): Int {
        return numberOfPlays.get()
    }

    fun setNumberOfPlays(plays: Int) {
        this.numberOfPlays.set(plays)
    }

    fun numberOfPlaysProperty(): IntegerProperty {
        return numberOfPlays
    }

    fun getDownloads(): Int {
        return downloads.get()
    }

    fun setDownloads(downloads: Int) {
        this.downloads.set(downloads)
    }

    fun downloadsProperty(): IntegerProperty {
        return downloads
    }

    fun getSize(): MapSize {
        return size.get()
    }

    fun setSize(size: MapSize) {
        this.size.set(size)
    }

    fun sizeProperty(): ObjectProperty<MapSize> {
        return size
    }

    fun getPlayers(): Int {
        return players.get()
    }

    fun setPlayers(players: Int) {
        this.players.set(players)
    }

    fun playersProperty(): IntegerProperty {
        return players
    }

    fun getVersion(): ComparableVersion? {
        return version.get()
    }

    fun setVersion(version: ComparableVersion) {
        this.version.set(version)
    }

    fun versionProperty(): ObjectProperty<ComparableVersion> {
        return version
    }

    override fun compareTo(o: MapBean): Int {
        return getDisplayName().compareTo(o.getDisplayName())
    }

    fun getDisplayName(): String {
        return displayName.get()
    }

    fun setDisplayName(displayName: String) {
        this.displayName.set(displayName)
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun getId(): String {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun getFolderName(): String {
        return folderName.get()
    }

    fun setFolderName(folderName: String) {
        this.folderName.set(folderName)
    }

    fun folderNameProperty(): StringProperty {
        return folderName
    }

    fun getLargeThumbnailUrl(): URL {
        return largeThumbnailUrl.get()
    }

    fun setLargeThumbnailUrl(largeThumbnailUrl: URL) {
        this.largeThumbnailUrl.set(largeThumbnailUrl)
    }

    fun largeThumbnailUrlProperty(): ObjectProperty<URL> {
        return largeThumbnailUrl
    }

    fun getSmallThumbnailUrl(): URL {
        return smallThumbnailUrl.get()
    }

    fun setSmallThumbnailUrl(smallThumbnailUrl: URL) {
        this.smallThumbnailUrl.set(smallThumbnailUrl)
    }

    fun smallThumbnailUrlProperty(): ObjectProperty<URL> {
        return smallThumbnailUrl
    }

    fun getCreateTime(): LocalDateTime {
        return createTime.get()
    }

    fun setCreateTime(createTime: LocalDateTime) {
        this.createTime.set(createTime)
    }

    fun createTimeProperty(): ObjectProperty<LocalDateTime> {
        return createTime
    }

    fun getType(): Type {
        return type.get()
    }

    fun setType(type: Type) {
        this.type.set(type)
    }

    fun typeProperty(): ObjectProperty<Type> {
        return type
    }

    fun getReviews(): ObservableList<Review> {
        return reviews.get()
    }

    fun reviewsProperty(): ListProperty<Review> {
        return reviews
    }

    fun hiddenProperty(): BooleanProperty {
        return hidden
    }

    fun rankedProperty(): BooleanProperty {
        return ranked
    }

    enum class Type private constructor(private val string: String) {
        SKIRMISH("skirmish"),
        COOP("campaign_coop"),
        OTHER(null);


        companion object {

            private val fromString: MutableMap<String, Type>

            init {
                fromString = HashMap()
                for (type in values()) {
                    fromString[type.string] = type
                }
            }

            fun fromString(type: String): Type {
                return if (fromString.containsKey(type)) {
                    fromString[type]
                } else OTHER
            }
        }
    }

    companion object {

        fun fromMapDto(map: com.faforever.client.api.dto.Map): MapBean {
            val mapVersion = map.getLatestVersion()

            val mapBean = MapBean()
            Optional.ofNullable(map.getAuthor()).ifPresent({ author -> mapBean.setAuthor(author.getLogin()) })
            mapBean.setDescription(mapVersion.getDescription())
            mapBean.setDisplayName(map.getDisplayName())
            mapBean.setFolderName(mapVersion.getFolderName())
            mapBean.setSize(MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()))
            mapBean.setDownloads(map.getStatistics().getDownloads())
            mapBean.setId(mapVersion.getId())
            mapBean.setPlayers(mapVersion.getMaxPlayers())
            mapBean.setVersion(mapVersion.getVersion())
            mapBean.setDownloadUrl(mapVersion.getDownloadUrl())
            mapBean.setSmallThumbnailUrl(mapVersion.getThumbnailUrlSmall())
            mapBean.setLargeThumbnailUrl(mapVersion.getThumbnailUrlLarge())
            mapBean.setCreateTime(mapVersion.getCreateTime().toLocalDateTime())
            mapBean.setNumberOfPlays(map.getStatistics().getPlays())
            mapBean.isRanked = mapVersion.getRanked()
            mapBean.isHidden = mapVersion.getHidden()
            mapBean.getReviews().setAll(
                    map.getVersions().stream()
                            .filter({ v -> v.getReviews() != null })
                            .flatMap({ v -> v.getReviews().parallelStream() })
                            .map(???({ Review.fromDto(it) }))
            .collect(Collectors.toList<T>()))
            return mapBean
        }

        fun fromMapVersionDto(mapVersion: com.faforever.client.api.dto.MapVersion): MapBean {
            val mapBean = MapBean()
            Optional.ofNullable(mapVersion.getMap().getAuthor()).ifPresent({ author -> mapBean.setAuthor(author.getLogin()) })
            mapBean.setDescription(mapVersion.getDescription())
            mapBean.setDisplayName(mapVersion.getMap().getDisplayName())
            mapBean.setFolderName(mapVersion.getFolderName())
            mapBean.setSize(MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()))
            mapBean.setDownloads(mapVersion.getMap().getStatistics().getDownloads())
            mapBean.setId(mapVersion.getId())
            mapBean.setPlayers(mapVersion.getMaxPlayers())
            mapBean.setVersion(mapVersion.getVersion())
            mapBean.setDownloadUrl(mapVersion.getDownloadUrl())
            mapBean.setSmallThumbnailUrl(mapVersion.getThumbnailUrlSmall())
            mapBean.setLargeThumbnailUrl(mapVersion.getThumbnailUrlLarge())
            mapBean.setCreateTime(mapVersion.getCreateTime().toLocalDateTime())
            mapBean.setNumberOfPlays(mapVersion.getMap().getStatistics().getPlays())
            mapBean.getReviews().setAll(mapVersion.getReviews().parallelStream()
                    .map(???({ Review.fromDto(it) }))
            .collect(Collectors.toList<T>()))
            mapBean.isHidden = mapVersion.getHidden()
            mapBean.isRanked = mapVersion.getRanked()
            return mapBean
        }
    }
}
