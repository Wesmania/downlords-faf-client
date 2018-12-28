package com.faforever.client.mod

import com.faforever.client.vault.review.Review
import com.faforever.commons.mod.MountInfo
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

import tornadofx.getValue
import tornadofx.setValue

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors.toList

class ModVersion {
    val displayNameProperty: StringProperty = SimpleStringProperty()
    var displayName by displayNameProperty

    val imagePathProperty: ObjectProperty<Path> = SimpleObjectProperty()
    var imagePath by imagePathProperty

    /**
     * Entity ID as provided by the API (DB primary key).
     * The ID within the database. `null` in case the mod was loaded locally.
     */
    val idProperty: StringProperty = SimpleStringProperty()
    var id by idProperty

    /**
     * UID as specified in the mod itself (specified by the uploader).
     */
    val uidProperty: StringProperty = SimpleStringProperty()
    var uid: String by uidProperty

    val descriptionProperty: StringProperty = SimpleStringProperty()
    var description: String by descriptionProperty

    val uploaderProperty: StringProperty = SimpleStringProperty()
    var uploader: String by uploaderProperty

    val selectableProperty: BooleanProperty = SimpleBooleanProperty()
    var selectable: Boolean by selectableProperty

    val versionProperty: ObjectProperty<ComparableVersion> = SimpleObjectProperty()
    var version: ComparableVersion by versionProperty

    val thumbnailUrlProperty: ObjectProperty<URL> = SimpleObjectProperty()
    var thumbnailUrl: URL by thumbnailUrlProperty

    val commentsProperty: ListProperty<String> = SimpleListProperty(FXCollections.observableArrayList())
    var comments: ObservableList<String> by commentsProperty

    val selectedProperty: BooleanProperty = SimpleBooleanProperty()
    var selected: Boolean by selectedProperty

    val likesProperty: IntegerProperty = SimpleIntegerProperty()
    var likes: Int by likesProperty

    val playedProperty: IntegerProperty = SimpleIntegerProperty()
    var played: Int by playedProperty

    val createTimeProperty: ObjectProperty<LocalDateTime> = SimpleObjectProperty()
    var createTime: LocalDateTime by createTimeProperty

    val updateTimeProperty: ObjectProperty<LocalDateTime> = SimpleObjectProperty()
    var updateTime: LocalDateTime by updateTimeProperty

    val downloadUrlProperty: ObjectProperty<URL> = SimpleObjectProperty()
    var downloadUrl: URL by downloadUrlProperty

    val mountPointsProperty: ListProperty<MountInfo> = SimpleListProperty(FXCollections.observableArrayList())
    val mountInfos: ObservableList<MountInfo> by mountPointsProperty

    val hookDirectoriesProperty: ListProperty<String> = SimpleListProperty(FXCollections.observableArrayList())
    val hookDirectories: ObservableList<String> by hookDirectoriesProperty

    val reviewsProperty: ListProperty<Review> = SimpleListProperty(FXCollections.observableArrayList { arrayOf(it.scoreProperty(), it.textProperty()) })
    var reviews: ObservableList<Review> by reviewsProperty

    val reviewsSummaryProperty: ObjectProperty<ReviewsSummary> = SimpleObjectProperty()
    var reviewsSummary: ReviewsSummary by reviewsSummaryProperty

    val modTypeProperty: ObjectProperty<ModType> = SimpleObjectProperty()
    var modType: ModType by modTypeProperty

    val filenameProperty: StringProperty = SimpleStringProperty()
    var filename: String by filenameProperty

    val iconProperty: StringProperty = SimpleStringProperty()
    var icon: String by iconProperty

    val rankedProperty: BooleanProperty = SimpleBooleanProperty()
    var ranked: Boolean by rankedProperty

    val hiddenProperty: BooleanProperty = SimpleBooleanProperty()
    var hidden: Boolean by hiddenProperty

    val modProperty: ObjectProperty<Mod> = SimpleObjectProperty()
    var mod: Mod by modProperty

    override fun hashCode(): Int {
        return Objects.hash(uid)
    }

    override fun equals(o: Any?): Boolean {
        if (o is ModVersion && o.uid != null && uid != null) {
            return o.uid == this.uid
        }
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as ModVersion
        return uid == that.uid
    }

    enum class ModType private constructor(val i18nKey: String) {
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
            modVersion.uid = modInfo.uid
            modVersion.displayName = modInfo.name
            modVersion.description = modInfo.description
            modVersion.uploader = modInfo.author
            modVersion.version = modInfo.version
            modVersion.selectable = modInfo.isSelectable
            modVersion.modType = if (modInfo.isUiOnly) ModType.UI else ModType.SIM
            modVersion.mountInfos.setAll(modInfo.mountInfos)
            modVersion.hookDirectories.setAll(modInfo.hookDirectories)
            Optional.ofNullable(modInfo.icon)
                    .map { icon -> Paths.get(icon) }
                    .filter { iconPath -> iconPath.nameCount > 2 }
                    .ifPresent { iconPath -> modVersion.imagePath = basePath.resolve(iconPath.subpath(2, iconPath.nameCount)) }
            return modVersion
        }

        fun fromDto(dto: com.faforever.client.api.dto.ModVersion, parent: Mod): ModVersion {
            val modVersionVersion = ModVersion()
            modVersionVersion.version = dto.version
            modVersionVersion.id = dto.id
            modVersionVersion.uid = dto.uid
            modVersionVersion.modType = ModType.fromDto(dto.type)
            modVersionVersion.description = dto.description
            modVersionVersion.filename = dto.filename
            modVersionVersion.icon = dto.icon
            modVersionVersion.ranked = dto.ranked
            modVersionVersion.hidden = dto.hidden
            Optional.ofNullable(dto.createTime)
                    .ifPresent { modVersionVersion.createTime = it.toLocalDateTime() }
            Optional.ofNullable(dto.updateTime)
                    .ifPresent{ modVersionVersion.updateTime = it.toLocalDateTime() }
            modVersionVersion.thumbnailUrl = dto.thumbnailUrl
            modVersionVersion.downloadUrl = dto.downloadUrl
            modVersionVersion.mod = Optional.ofNullable(parent)
                    .orElseGet { Mod.fromDto(dto.mod) }
            return modVersionVersion
        }

        fun fromModDto(dto: com.faforever.client.api.dto.Mod): ModVersion {
            val modVersionVersion = dto.latestVersion

            val modVersion = ModVersion()
            Optional.ofNullable(dto.uploader).ifPresent { modVersion.uploader = it.login }
            modVersion.description = modVersionVersion.description
            modVersion.displayName = dto.displayName
            modVersion.id = modVersionVersion.id
            modVersion.uid = modVersionVersion.uid
            modVersion.version = modVersionVersion.version
            modVersion.downloadUrl = modVersionVersion.downloadUrl
            modVersion.thumbnailUrl = modVersionVersion.thumbnailUrl
            modVersion.reviewsSummary = ReviewsSummary.fromDto(modVersionVersion.modVersionReviewsSummary)
            modVersion.createTime = modVersionVersion.createTime.toLocalDateTime())
            Optional.ofNullable(dto.updateTime).map { it.toLocalDateTime() }.ifPresent { modVersion.updateTime = it }
            modVersion.reviews.setAll(
                    dto.versions!!.stream()
                            .filter { it.reviews != null }
                            .flatMap { it.reviews.parallelStream() }
                            .map { Review.fromDto(it) }
            .collect(toList()))
            modVersion.modType = ModType.fromDto(modVersionVersion.type)
            return modVersion
        }
    }
}
