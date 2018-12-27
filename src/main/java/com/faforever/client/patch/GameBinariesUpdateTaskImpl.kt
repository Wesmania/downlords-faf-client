package com.faforever.client.patch

import com.faforever.client.config.ClientProperties
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import com.faforever.client.util.Assert
import com.faforever.client.util.Validator
import com.faforever.commons.fa.ForgedAllianceExePatcher
import com.faforever.commons.io.ByteCopier
import com.google.common.annotations.VisibleForTesting
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays

import com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE
import com.github.nocatch.NoCatch.noCatch
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.Files.setAttribute
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GameBinariesUpdateTaskImpl @Inject
constructor(private val i18n: I18n, private val preferencesService: PreferencesService, clientProperties: ClientProperties) : CompletableTask<Void>(CompletableTask.Priority.HIGH), GameBinariesUpdateTask {

    private val fafExeUrl: String

    private var version: Int? = null

    init {

        this.fafExeUrl = clientProperties.getForgedAlliance().getExeUrl()
    }

    @Throws(Exception::class)
    public override fun call(): Void? {
        updateTitle(i18n.get("updater.binary.taskTitle"))
        Assert.checkNullIllegalState(version, "Field 'version' must not be null")
        logger.info("Updating binaries to {}", version)

        val exePath = preferencesService.fafBinDirectory.resolve(FORGED_ALLIANCE_EXE)

        copyGameFilesToFafBinDirectory()
        downloadFafExeIfNecessary(exePath)
        ForgedAllianceExePatcher.patchVersion(exePath, version!!)
        logger.debug("Binaries have been updated successfully")
        return null
    }

    @Throws(IOException::class)
    private fun downloadFafExeIfNecessary(exePath: Path) {
        if (Files.exists(exePath)) {
            return
        }
        ResourceLocks.acquireDownloadLock()
        try {
            logger.debug("Downloading {} to {}", fafExeUrl, exePath)
            val urlConnection = URL(fafExeUrl).openConnection()
            urlConnection.getInputStream().use { inputStream ->
                Files.newOutputStream(exePath).use { outputStream ->
                    ByteCopier.from(inputStream)
                            .to(outputStream)
                            .totalBytes(urlConnection.contentLength.toLong())
                            .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                            .copy()
                }
            }
        } finally {
            ResourceLocks.freeDownloadLock()
        }
    }

    @VisibleForTesting
    @Throws(IOException::class)
    internal fun copyGameFilesToFafBinDirectory() {
        logger.debug("Copying Forged Alliance binaries FAF folder")

        val fafBinDirectory = preferencesService.fafBinDirectory
        createDirectories(fafBinDirectory)

        val faBinPath = preferencesService.preferences!!.forgedAlliance.path.resolve("bin")

        Files.list(faBinPath)
                .filter { path -> BINARIES_TO_COPY.contains(path.fileName.toString()) }
                .forEach { source ->
                    val destination = fafBinDirectory.resolve(source.fileName)

                    logger.debug("Copying file '{}' to '{}'", source, destination)
                    noCatch<Path> { createDirectories(destination.parent) }
                    noCatch<Path> { copy(source, destination, REPLACE_EXISTING) }

                    if (org.bridj.Platform.isWindows()) {
                        noCatch<Path> { setAttribute(destination, "dos:readonly", false) }
                    }
                }
    }

    override fun setVersion(version: ComparableVersion) {
        val versionString = version.toString()
        if (!Validator.isInt(versionString)) {
            throw IllegalArgumentException("Versions of featured preferences mods must be integers")
        }
        this.version = Integer.parseInt(versionString)
    }

    companion object {

        @VisibleForTesting
        internal val BINARIES_TO_COPY: Collection<String> = Arrays.asList(
                "BsSndRpt.exe",
                "BugSplat.dll",
                "BugSplatRc.dll",
                "DbgHelp.dll",
                "GDFBinary.dll",
                "Microsoft.VC80.CRT.manifest",
                "SHSMP.DLL",
                "msvcm80.dll",
                "msvcp80.dll",
                "msvcr80.dll",
                "sx32w.dll",
                "wxmsw24u-vs80.dll",
                "zlibwapi.dll"
        )
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
