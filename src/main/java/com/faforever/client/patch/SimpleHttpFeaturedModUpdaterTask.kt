package com.faforever.client.patch

import com.faforever.client.api.dto.FeaturedModFile
import com.faforever.client.i18n.I18n
import com.faforever.client.io.DownloadService
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.task.CompletableTask
import com.google.common.hash.Hashing
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.lang.invoke.MethodHandles
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SimpleHttpFeaturedModUpdaterTask(private val fafService: FafService, private val preferencesService: PreferencesService, private val downloadService: DownloadService, private val i18n: I18n) : CompletableTask<PatchResult>(CompletableTask.Priority.HIGH) {

    private var featuredMod: FeaturedMod? = null
    private var version: Int? = null

    @Throws(Exception::class)
    override fun call(): PatchResult {
        val initFileName = "init_" + featuredMod!!.technicalName + ".lua"

        updateTitle(i18n.get("updater.taskTitle"))
        updateMessage(i18n.get("updater.readingFileList"))

        val featuredModFiles = fafService.getFeaturedModFiles(featuredMod, version).get()

        var initFile: Path? = null
        for (featuredModFile in featuredModFiles) {
            val targetPath = preferencesService.fafDataDirectory
                    .resolve(featuredModFile.getGroup())
                    .resolve(featuredModFile.getName())

            if (Files.exists(targetPath) && featuredModFile.getMd5().equals(com.google.common.io.Files.hash(targetPath.toFile(), Hashing.md5()).toString())) {
                logger.debug("Already up to date: {}", targetPath)
            } else {
                Files.createDirectories(targetPath.getParent())
                updateMessage(i18n.get("updater.downloadingFile", targetPath.getFileName()))
                downloadService.downloadFile(URL(featuredModFile.getUrl()), targetPath, ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
            }

            if ("bin" == featuredModFile.getGroup() && initFileName.equals(featuredModFile.getName(), ignoreCase = true)) {
                initFile = targetPath
            }
        }

        Assert.isTrue(initFile != null && Files.exists(initFile), "'$initFileName' could be found.")

        val maxVersion = featuredModFiles.stream()
                .mapToInt { mod -> Integer.parseInt(mod.getVersion()) }
                .max()
                .orElseThrow { IllegalStateException("No version found") }

        return PatchResult.withLegacyInitFile(ComparableVersion(maxVersion.toString()), initFile)
    }

    fun setFeaturedMod(featuredMod: FeaturedMod) {
        this.featuredMod = featuredMod
    }

    fun setVersion(version: Int?) {
        this.version = version
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
