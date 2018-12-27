package com.faforever.client.mod

import com.faforever.client.vault.review.Review
import com.faforever.commons.mod.MountInfo
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
import lombok.Getter
import org.apache.maven.artifact.versioning.ComparableVersion

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

class ModVersion {
    private val displayName: StringProperty
    private val imagePath: ObjectProperty<Path>
    /**
     * Entity ID as provided by the API (DB primary key).
     */
    private val id: StringProperty
    /**
     * UID as specified in the mod itself (specified by the uploader).
     */
    private val uid: StringProperty
    private val description: StringProperty
    private val uploader: StringProperty
    private val selectable: BooleanProperty
    private val version: ObjectProperty<ComparableVersion>
    private val thumbnailUrl: ObjectProperty<URL>
    private val comments: ListProperty<String>
    private val selected: BooleanProperty
    private val likes: IntegerProperty
    private val played: IntegerProperty
    private val createTime: ObjectProperty<LocalDateTime>
    private val updateTime: ObjectProperty<LocalDateTime>
    private val downloadUrl: ObjectProperty<URL>
    private val mountPoints: ListProperty<MountInfo>
    private val hookDirectories: ListProperty<String>
    private val reviews: ListProperty<Review>
    private val reviewsSummary: ObjectProperty<ReviewsSummary>
    private val modType: ObjectProperty<ModType>
    private val filename: StringProperty
    private val icon: StringProperty
    private val ranked: BooleanProperty
    private val hidden: BooleanProperty
    private val mod: ObjectProperty<Mod>

    val mountInfos: ObservableList<MountInfo>
        get() = mountPoints.get()

    var isRanked: Boolean
        get() = ranked.get()
        set(ranked) = this.ranked.set(ranked)

    var isHidden: Boolean
        get() = hidden.get()
        set(hidden) = this.hidden.set(hidden)

    init {
        displayName = SimpleStringProperty()
        imagePath = SimpleObjectProperty()
        id = SimpleStringProperty()
        uid = SimpleStringProperty()
        description = SimpleStringProperty()
        uploader = SimpleStringProperty()
        selectable = SimpleBooleanProperty()
        version = SimpleObjectProperty()
        selected = SimpleBooleanProperty()
        likes = SimpleIntegerProperty()
        played = SimpleIntegerProperty()
        createTime = SimpleObjectProperty()
        updateTime = SimpleObjectProperty()
        reviewsSummary = SimpleObjectProperty()
        thumbnailUrl = SimpleObjectProperty()
        comments = SimpleListProperty(FXCollections.observableArrayList())
        downloadUrl = SimpleObjectProperty()
        mountPoints = SimpleListProperty(FXCollections.observableArrayList())
        hookDirectories = SimpleListProperty(FXCollections.observableArrayList())
        reviews = SimpleListProperty(FXCollections.observableArrayList { param -> arrayOf(param.scoreProperty(), param.textProperty()) })
        modType = SimpleObjectProperty()
        filename = SimpleStringProperty()
        icon = SimpleStringProperty()
        ranked = SimpleBooleanProperty()
        hidden = SimpleBooleanProperty()
        mod = SimpleObjectProperty()
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

    fun getSelected(): Boolean {
        return selected.get()
    }

    fun setSelected(selected: Boolean) {
        this.selected.set(selected)
    }

    fun selectedProperty(): BooleanProperty {
        return selected
    }

    fun getUploader(): String {
        return uploader.get()
    }

    fun setUploader(uploader: String) {
        this.uploader.set(uploader)
    }

    fun getSelectable(): Boolean {
        return selectable.get()
    }

    fun setSelectable(selectable: Boolean) {
        this.selectable.set(selectable)
    }

    fun selectableProperty(): BooleanProperty {
        return selectable
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

    fun getVersion(): ComparableVersion {
        return version.get()
    }

    fun setVersion(version: ComparableVersion) {
        this.version.set(version)
    }

    fun versionProperty(): ObjectProperty<ComparableVersion> {
        return version
    }

    fun getImagePath(): Path {
        return imagePath.get()
    }

    fun setImagePath(imagePath: Path) {
        this.imagePath.set(imagePath)
    }

    fun imagePathProperty(): ObjectProperty<Path> {
        return imagePath
    }

    fun uploaderProperty(): StringProperty {
        return uploader
    }

    fun getDisplayName(): String {
        return displayName.get()
    }

    fun setDisplayName(displayName: String) {
        this.displayName.set(displayName)
    }

    /**
     * The ID within the database. `null` in case the mod was loaded locally.
     */
    fun getId(): String? {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun getLikes(): Int {
        return likes.get()
    }

    fun setLikes(likes: Int) {
        this.likes.set(likes)
    }

    fun likesProperty(): IntegerProperty {
        return likes
    }

    fun getPlayed(): Int {
        return played.get()
    }

    fun setPlayed(played: Int) {
        this.played.set(played)
    }

    fun playedProperty(): IntegerProperty {
        return played
    }

    fun displayNameProperty(): StringProperty {
        return displayName
    }

    fun getCreateTime(): LocalDateTime {
        return createTime.get()
    }

    fun setCreateTime(createTime: LocalDateTime) {
        this.createTime.set(createTime)
    }

    fun getThumbnailUrl(): URL {
        return thumbnailUrl.get()
    }

    fun setThumbnailUrl(thumbnailUrl: URL) {
        this.thumbnailUrl.set(thumbnailUrl)
    }

    fun thumbnailUrlProperty(): ObjectProperty<URL> {
        return thumbnailUrl
    }

    fun commentsProperty(): ListProperty<String> {
        return comments
    }

    override fun hashCode(): Int {
        return Objects.hash(uid.get())
    }

    override fun equals(o: Any?): Boolean {
        if (o is ModVersion && o.getUid() != null && getUid() != null) {
            return o.getUid() == this.getUid()
        }
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as ModVersion?
        return uid.get() == that!!.uid.get()
    }

    fun getComments(): ObservableList<String> {
        return comments.get()
    }

    fun setComments(comments: ObservableList<String>) {
        this.comments.set(comments)
    }

    fun getHookDirectories(): ObservableList<String> {
        return hookDirectories.get()
    }

    fun getReviews(): ObservableList<Review> {
        return reviews.get()
    }

    fun setReviews(reviews: ObservableList<Review>) {
        this.reviews.set(reviews)
    }

    fun reviewsProperty(): ListProperty<Review> {
        return reviews
    }

    fun getUid(): String? {
        return uid.get()
    }

    fun setUid(uid: String) {
        this.uid.set(uid)
    }

    fun uidProperty(): StringProperty {
        return uid
    }

    fun createTimeProperty(): ObjectProperty<LocalDateTime> {
        return createTime
    }

    fun getUpdateTime(): LocalDateTime {
        return updateTime.get()
    }

    fun setUpdateTime(updateTime: LocalDateTime) {
        this.updateTime.set(updateTime)
    }

    fun updateTimeProperty(): ObjectProperty<LocalDateTime> {
        return updateTime
    }

    fun getReviewsSummary(): ReviewsSummary {
        return reviewsSummary.get()
    }

    fun setReviewsSummary(reviewsSummary: ReviewsSummary) {
        this.reviewsSummary.set(reviewsSummary)
    }

    fun reviewsSummaryProperty(): ObjectProperty<ReviewsSummary> {
        return reviewsSummary
    }

    fun getModType(): ModType {
        return modType.get()
    }

    fun setModType(modType: ModType) {
        this.modType.set(modType)
    }

    fun modTypeProperty(): ObjectProperty<ModType> {
        return modType
    }

    fun getFilename(): String {
        return filename.get()
    }

    fun setFilename(filename: String) {
        this.filename.set(filename)
    }

    fun filenameProperty(): StringProperty {
        return filename
    }

    fun getIcon(): String {
        return icon.get()
    }

    fun setIcon(icon: String) {
        this.icon.set(icon)
    }

    fun iconProperty(): StringProperty {
        return icon
    }

    fun rankedProperty(): BooleanProperty {
        return ranked
    }

    fun hiddenProperty(): BooleanProperty {
        return hidden
    }

    fun getMod(): Mod {
        return mod.get()
    }

    fun setMod(mod: Mod) {
        this.mod.set(mod)
    }

    fun modProperty(): ObjectProperty<Mod> {
        return mod
    }

    enum class ModType private constructor(@field:Getter
                                           private val i18nKey: String) {
        UI("modType.ui"),
        SIM("modType.sim");


        companion object {

            fun fromDto(modType: com.faforever.client.api.dto.ModType): ModType {
                return if (modType === com.faforever.client.api.dto.ModType.UI) UI else SIM
            }
        }
    }

    companion object {

        /**
         * @param basePath path to the directory where all the mod files are, used to resolve the path of the icon file.
         */
        internal fun fromModInfo(modInfo: com.faforever.commons.mod.Mod, basePath: Path): ModVersion {
            val modVersion = ModVersion()
            modVersion.setUid(modInfo.uid)
            modVersion.setDisplayName(modInfo.name)
            modVersion.setDescription(modInfo.description)
            modVersion.setUploader(modInfo.author)
            modVersion.setVersion(modInfo.version)
            modVersion.setSelectable(modInfo.isSelectable)
            modVersion.setModType(if (modInfo.isUiOnly) ModType.UI else ModType.SIM)
            modVersion.mountInfos.setAll(modInfo.mountInfos)
            modVersion.getHookDirectories().setAll(modInfo.hookDirectories)
            Optional.ofNullable(modInfo.icon)
                    .map { icon -> Paths.get(icon) }
                    .filter { iconPath -> iconPath.nameCount > 2 }
                    .ifPresent { iconPath -> modVersion.setImagePath(basePath.resolve(iconPath.subpath(2, iconPath.nameCount))) }
            return modVersion
        }

        fun fromDto(dto: com.faforever.client.api.dto.ModVersion, parent: Mod): ModVersion {
            val modVersionVersion = ModVersion()
            modVersionVersion.setVersion(dto.getVersion())
            modVersionVersion.setId(dto.getId())
            modVersionVersion.setUid(dto.getUid())
            modVersionVersion.setModType(ModType.fromDto(dto.getType()))
            modVersionVersion.setDescription(dto.getDescription())
            modVersionVersion.setFilename(dto.getFilename())
            modVersionVersion.setIcon(dto.getIcon())
            modVersionVersion.isRanked = dto.isRanked()
            modVersionVersion.isHidden = dto.isHidden()
            Optional.ofNullable(dto.getCreateTime())
                    .ifPresent({ offsetDateTime -> modVersionVersion.setCreateTime(offsetDateTime.toLocalDateTime()) })
            Optional.ofNullable(dto.getUpdateTime())
                    .ifPresent({ offsetDateTime -> modVersionVersion.setUpdateTime(offsetDateTime.toLocalDateTime()) })
            modVersionVersion.setThumbnailUrl(dto.getThumbnailUrl())
            modVersionVersion.setDownloadUrl(dto.getDownloadUrl())
            val mod = Optional.ofNullable(parent)
                    .orElseGet { Mod.fromDto(dto.getMod()) }
            modVersionVersion.setMod(mod)
            return modVersionVersion
        }

        fun fromModDto(dto: com.faforever.client.api.dto.Mod): ModVersion {
            val modVersionVersion = dto.getLatestVersion()

            val modVersion = ModVersion()
            Optional.ofNullable(dto.getUploader()).ifPresent({ uploader -> modVersion.setUploader(uploader.getLogin()) })
            modVersion.setDescription(modVersionVersion.getDescription())
            modVersion.setDisplayName(dto.getDisplayName())
            modVersion.setId(modVersionVersion.getId())
            modVersion.setUid(modVersionVersion.getUid())
            modVersion.setVersion(modVersionVersion.getVersion())
            modVersion.setDownloadUrl(modVersionVersion.getDownloadUrl())
            modVersion.setThumbnailUrl(modVersionVersion.getThumbnailUrl())
            modVersion.setReviewsSummary(ReviewsSummary.fromDto(modVersionVersion.getModVersionReviewsSummary()))
            modVersion.setCreateTime(modVersionVersion.getCreateTime().toLocalDateTime())
            Optional.ofNullable(dto.getUpdateTime()).map(Function<T, U> { it.toLocalDateTime() }).ifPresent(Consumer<U> { modVersion.setUpdateTime(it) })
            modVersion.getReviews().setAll(
                    dto.getVersions().stream()
                            .filter({ v -> v.getReviews() != null })
                            .flatMap({ v -> v.getReviews().parallelStream() })
                            .map(???({ Review.fromDto(it) }))
            .collect(Collectors.toList<T>()))
            modVersion.setModType(ModType.fromDto(modVersionVersion.getType()))
            return modVersion
        }
    }
}
