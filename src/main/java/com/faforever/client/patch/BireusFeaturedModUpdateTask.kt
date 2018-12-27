package com.faforever.client.patch

import com.faforever.client.i18n.I18n
import com.faforever.client.io.DownloadService
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModService
import com.faforever.client.mod.ModVersion
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import net.brutus5000.bireus.BireusClient
import net.brutus5000.bireus.service.PatchEventListener
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class BireusFeaturedModUpdateTask(private val preferencesService: PreferencesService, downloadService: DownloadService, i18n: I18n, private val modService: ModService) : CompletableTask<PatchResult>(CompletableTask.Priority.HIGH) {
    private val patchEventListener: PatchEventListener
    private val bireusDownloadService: BireusDownloadServiceAdapter
    private var version: Int? = null
    private var featuredMod: FeaturedMod? = null
    private var checkedOutVersion: String? = null

    init {
        bireusDownloadService = BireusDownloadServiceAdapter(downloadService, ByteCountListener { l, l1 -> this.updateProgress(l, l1) })

        patchEventListener = object : PatchEventListener {

            override fun error(message: String?) {
                // TODO display message to user
            }

            override fun beginCheckoutVersion(version: String?) {
                updateTitle(i18n.get("updater.taskTitle"))
            }

            override fun beginDownloadPatch(url: URL?) {
                updateMessage(i18n.get("updater.downloadingFile", url))
            }

            override fun beginPatchingFile(path: Path?) {
                updateMessage(i18n.get("updater.patchingFile", path))
            }

            override fun crcMismatch(patchPath: Path?) {
                // TODO display message to user
            }

            override fun checkedOutAlready(version: String?) {
                this@BireusFeaturedModUpdateTask.checkedOutVersion = version
            }

            override fun finishCheckoutVersion(version: String?) {
                this@BireusFeaturedModUpdateTask.checkedOutVersion = version
            }
        }
    }


    @Throws(Exception::class)
    override fun call(): PatchResult {
        val repoDirName = featuredMod!!.bireusUrl.toString().replace(NON_WORD_CHARACTER_PATTERN.toRegex(), "")
        val repositoryPath = preferencesService.patchReposDirectory.resolve(repoDirName)

        ResourceLocks.acquireDiskLock()
        try {
            val bireusClient = initBireus(repositoryPath)
            if (version == null) {
                bireusClient.checkoutLatestVersion()
            } else {
                bireusClient.checkoutVersion(version.toString())
            }
        } finally {
            ResourceLocks.freeDiskLock()
        }

        val modInfoLuaFile = repositoryPath.resolve(MOD_INFO_LUA)

        // Older versions do not have a mod info file. Their init file can't be generated, so we require the mod to provide its own
        if (!Files.exists(modInfoLuaFile)) {
            val initFile = repositoryPath.resolve("bin/init_" + featuredMod!!.technicalName + ".lua")
            Assert.isTrue(Files.exists(initFile), "Neither '" + MOD_INFO_LUA + "' nor '" + initFile.fileName + "' could be found.")

            return PatchResult.withLegacyInitFile(ComparableVersion(checkedOutVersion!!), initFile)
        }

        Files.newInputStream(modInfoLuaFile).use { inputStream ->
            val modVersion = modService.extractModInfo(inputStream, repositoryPath)
            return PatchResult.fromModInfo(modService.readModVersion(repositoryPath), modVersion.mountInfos, modVersion.hookDirectories)
        }
    }

    @SneakyThrows
    private fun initBireus(repositoryPath: Path): BireusClient {
        if (Files.notExists(repositoryPath)) {
            Files.createDirectories(repositoryPath)
            return BireusClient.getFromURL(featuredMod!!.bireusUrl, repositoryPath, patchEventListener, bireusDownloadService)
        }
        return BireusClient(repositoryPath, patchEventListener, bireusDownloadService)
    }

    fun setVersion(version: Int?) {
        this.version = version
    }

    fun setFeaturedMod(featuredMod: FeaturedMod) {
        this.featuredMod = featuredMod
    }

    companion object {

        private val NON_WORD_CHARACTER_PATTERN = "[^\\w]"
        private val MOD_INFO_LUA = "mod_info.lua"
    }
}
