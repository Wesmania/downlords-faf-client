package com.faforever.client.mod

import com.faforever.client.config.CacheNames
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.i18n.I18n
import com.faforever.client.mod.ModVersion.ModType
import com.faforever.client.notification.Action
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.query.SearchableProperties
import com.faforever.client.remote.AssetService
import com.faforever.client.remote.FafService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import com.faforever.client.util.IdenticonUtil
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.client.vault.search.SearchController.SortOrder
import com.faforever.commons.mod.ModLoadException
import com.faforever.commons.mod.ModReader
import javafx.beans.property.DoubleProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
import lombok.SneakyThrows
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Inject
import java.io.IOException
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

import com.faforever.client.notification.Severity.WARN
import com.github.nocatch.NoCatch.noCatch
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.file.Files.createDirectories
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.util.Collections.singletonList
import java.util.concurrent.CompletableFuture.completedFuture

@Lazy
@Service
class ModService @Inject
constructor(private val taskService: TaskService, private val fafService: FafService, private val preferencesService: PreferencesService,
            private val applicationContext: ApplicationContext,
            private val notificationService: NotificationService, private val i18n: I18n,
            private val platformService: PlatformService, private val assetService: AssetService) {
    private val modReader: ModReader

    private var modsDirectory: Path? = null
    private val pathToMod: MutableMap<Path, ModVersion>
    private val installedModVersions: ObservableList<ModVersion>
    private val readOnlyInstalledModVersions: ObservableList<ModVersion>
    private var directoryWatcherThread: Thread? = null

    val installedModUids: Set<String>
        get() = getInstalledModVersions().stream()
                .map<String>(Function<ModVersion, String> { it.getUid() })
                .collect<Set<String>, Any>(Collectors.toSet())

    val installedUiModsUids: Set<String>
        get() = getInstalledModVersions().stream()
                .filter { mod -> mod.modType == ModType.UI }
                .map<String>(Function<ModVersion, String> { it.getUid() })
                .collect<Set<String>, Any>(Collectors.toSet())

    val featuredMods: CompletableFuture<List<FeaturedMod>>
        get() = fafService.featuredMods

    val activatedSimAndUIMods: List<ModVersion>
        @Throws(IOException::class)
        get() {
            val modStates = readModStates()
            return getInstalledModVersions().parallelStream()
                    .filter { mod -> modStates.containsKey(mod.uid) && modStates[mod.uid] }
                    .collect<List<ModVersion>, Any>(Collectors.toList())
        }

    init {
        pathToMod = HashMap()
        modReader = ModReader()
        installedModVersions = FXCollections.observableArrayList()
        readOnlyInstalledModVersions = FXCollections.unmodifiableObservableList(installedModVersions)
    }// TODO divide and conquer

    @PostConstruct
    internal fun postConstruct() {
        modsDirectory = preferencesService.preferences!!.forgedAlliance.modsDirectory
        JavaFxUtil.addListener(preferencesService.preferences!!.forgedAlliance.modsDirectoryProperty()) { observable, oldValue, newValue ->
            if (newValue != null) {
                onModDirectoryReady()
            }
        }

        if (modsDirectory != null) {
            onModDirectoryReady()
        }
    }

    private fun onModDirectoryReady() {
        try {
            createDirectories(modsDirectory)
            directoryWatcherThread = startDirectoryWatcher(modsDirectory)
        } catch (e: IOException) {
            logger.warn("Could not start mod directory watcher", e)
            // TODO notify user
        }

        loadInstalledMods()
    }

    private fun startDirectoryWatcher(modsDirectory: Path?): Thread {
        val thread = Thread {
            noCatch {
                val watcher = modsDirectory!!.fileSystem.newWatchService()
                modsDirectory.register(watcher, ENTRY_DELETE)

                try {
                    while (!Thread.interrupted()) {
                        val key = watcher.take()
                        key.pollEvents().stream()
                                .filter { event -> event.kind() === ENTRY_DELETE }
                                .forEach { event -> removeMod(modsDirectory.resolve(event.context() as Path)) }
                        key.reset()
                    }
                } catch (e: InterruptedException) {
                    logger.debug("Watcher terminated ({})", e.message)
                }
            }
        }
        thread.start()
        return thread
    }

    fun loadInstalledMods() {
        try {
            Files.newDirectoryStream(modsDirectory!!) { entry -> Files.isDirectory(entry) }.use { directoryStream ->
                for (path in directoryStream) {
                    addMod(path)
                }
            }
        } catch (e: IOException) {
            logger.warn("Mods could not be read from: " + modsDirectory!!, e)
        }

    }

    fun getInstalledModVersions(): ObservableList<ModVersion> {
        return readOnlyInstalledModVersions
    }

    @SneakyThrows
    fun downloadAndInstallMod(uid: String): CompletableFuture<Void> {
        return fafService.getModVersion(uid)
                .thenCompose { mod -> downloadAndInstallMod(mod, null, null) }
                .exceptionally { throwable ->
                    logger.warn("Sim mod could not be installed", throwable)
                    null
                }
    }

    @JvmOverloads
    fun downloadAndInstallMod(url: URL, progressProperty: DoubleProperty? = null, titleProperty: StringProperty? = null): CompletableFuture<Void> {
        val task = applicationContext.getBean(InstallModTask::class.java)
        task.setUrl(url)
        progressProperty?.bind(task.progressProperty())
        titleProperty?.bind(task.titleProperty())

        return taskService.submitTask(task).future
                .thenRun { this.loadInstalledMods() }
    }

    fun downloadAndInstallMod(modVersion: ModVersion, progressProperty: DoubleProperty?, titleProperty: StringProperty?): CompletableFuture<Void> {
        return downloadAndInstallMod(modVersion.downloadUrl, progressProperty, titleProperty)
    }

    @Throws(IOException::class)
    fun enableSimMods(simMods: Set<String>) {
        val modStates = readModStates()

        val installedUiMods = installedUiModsUids

        for ((key, value) in modStates) {
            val uid = entry.key

            if (!installedUiMods.contains(uid)) {
                // Only disable it if it's a sim mod; because it has not been selected
                entry.setValue(false)
            }
        }
        for (simModUid in simMods) {
            modStates[simModUid] = true
        }

        writeModStates(modStates)
    }

    fun isModInstalled(uid: String): Boolean {
        return installedModUids.contains(uid)
    }

    fun uninstallMod(modVersion: ModVersion): CompletableFuture<Void> {
        val task = applicationContext.getBean(UninstallModTask::class.java)
        task.setModVersion(modVersion)
        return taskService.submitTask(task).future
    }

    fun getPathForMod(modVersionToFind: ModVersion): Path {
        return pathToMod.entries.stream()
                .filter { pathModEntry -> pathModEntry.value.uid == modVersionToFind.uid }
                .findFirst()
                .map<Path>(Function<Entry<Path, ModVersion>, Path> { it.key })
                .orElse(null)
    }

    fun getNewestMods(count: Int, page: Int): CompletableFuture<List<ModVersion>> {
        return findByQuery(SearchConfig(SortConfig(SearchableProperties.NEWEST_MOD_KEY, SortOrder.DESC), ""), page, count)
    }

    @SneakyThrows
    fun extractModInfo(path: Path): ModVersion {
        val modInfoLua = path.resolve("mod_info.lua")
        logger.debug("Reading mod {}", path)
        if (Files.notExists(modInfoLua)) {
            throw ModLoadException("Missing mod_info.lua in: " + path.toAbsolutePath())
        }

        Files.newInputStream(modInfoLua).use { inputStream -> return extractModInfo(inputStream, path) }
    }

    fun extractModInfo(inputStream: InputStream, basePath: Path): ModVersion {
        return ModVersion.fromModInfo(modReader.readModInfo(inputStream, basePath), basePath)
    }

    fun uploadMod(modPath: Path): CompletableTask<Void> {
        val modUploadTask = applicationContext.getBean(ModUploadTask::class.java)
        modUploadTask.setModPath(modPath)

        return taskService.submitTask(modUploadTask)
    }

    fun loadThumbnail(modVersion: ModVersion): Image? {
        //FIXME: reintroduce correct caching
        val url = modVersion.thumbnailUrl
        return assetService.loadAndCacheImage(url, Paths.get("mods")) { IdenticonUtil.createIdenticon(modVersion.displayName) }
    }

    fun evictModsCache() {
        fafService.evictModsCache()
    }

    /**
     * Returns the download size of the specified modVersion in bytes.
     */
    @SneakyThrows
    fun getModSize(modVersion: ModVersion): Long {
        var conn: HttpURLConnection? = null
        try {
            conn = modVersion.downloadUrl.openConnection() as HttpURLConnection
            conn.requestMethod = HttpMethod.HEAD.name
            return conn.contentLength.toLong()
        } finally {
            conn?.disconnect()
        }
    }

    fun readModVersion(modDirectory: Path): ComparableVersion {
        return extractModInfo(modDirectory).version
    }

    fun getFeaturedMod(featuredMod: String): CompletableFuture<FeaturedMod> {
        return featuredMods.thenCompose { featuredModBeans ->
            completedFuture(featuredModBeans.stream()
                    .filter { mod -> featuredMod == mod.technicalName }
                    .findFirst()
                    .orElseThrow { IllegalArgumentException("Not a valid featured mod: $featuredMod") }
            )
        }
    }

    fun findByQuery(searchConfig: SearchConfig, page: Int, count: Int): CompletableFuture<List<ModVersion>> {
        return fafService.findModsByQuery(searchConfig, page, count)
    }

    @CacheEvict(value = CacheNames.MODS, allEntries = true)
    fun evictCache() {
        // Nothing to see here
    }

    @Async
    fun getHighestRatedUiMods(count: Int, page: Int): CompletableFuture<List<ModVersion>> {
        return fafService.findModsByQuery(SearchConfig(SortConfig(SearchableProperties.HIGHEST_RATED_MOD_KEY, SortOrder.DESC), "latestVersion.type==UI"), page, count)
    }

    fun getHighestRatedMods(count: Int, page: Int): CompletableFuture<List<ModVersion>> {
        return fafService.findModsByQuery(SearchConfig(SortConfig(SearchableProperties.HIGHEST_RATED_MOD_KEY, SortOrder.DESC), ""), page, count)
    }

    @Throws(IOException::class)
    fun overrideActivatedMods(modVersions: List<ModVersion>) {
        val modStates = modVersions.parallelStream().collect<Map<String, Boolean>, Any>(Collectors.toMap(Function<ModVersion, String> { it.getUid() }, { o -> true }))
        writeModStates(modStates)
    }

    @Throws(IOException::class)
    private fun readModStates(): MutableMap<String, Boolean> {
        val preferencesFile = preferencesService.preferences!!.forgedAlliance.preferencesFile
        val mods = HashMap<String, Boolean>()

        val preferencesContent = String(Files.readAllBytes(preferencesFile), US_ASCII)
        val matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent)
        if (matcher.find()) {
            val activeModMatcher = ACTIVE_MOD_PATTERN.matcher(matcher.group(0))
            while (activeModMatcher.find()) {
                val modUid = activeModMatcher.group(1)
                val enabled = java.lang.Boolean.parseBoolean(activeModMatcher.group(2))

                mods[modUid] = enabled
            }
        }

        return mods
    }

    @Throws(IOException::class)
    private fun writeModStates(modStates: Map<String, Boolean>) {
        val preferencesFile = preferencesService.preferences!!.forgedAlliance.preferencesFile
        var preferencesContent = String(Files.readAllBytes(preferencesFile), US_ASCII)

        var currentActiveModsContent: String? = null
        val matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent)
        if (matcher.find()) {
            currentActiveModsContent = matcher.group(0)
        }

        val newActiveModsContentBuilder = StringBuilder("active_mods = {")

        val iterator = modStates.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value) {
                continue
            }

            newActiveModsContentBuilder.append("\n    ['")
            newActiveModsContentBuilder.append(entry.key)
            newActiveModsContentBuilder.append("'] = true")
            if (iterator.hasNext()) {
                newActiveModsContentBuilder.append(",")
            }
        }
        newActiveModsContentBuilder.append("\n}")

        if (currentActiveModsContent != null) {
            preferencesContent = preferencesContent.replace(currentActiveModsContent, newActiveModsContentBuilder)
        } else {
            preferencesContent += newActiveModsContentBuilder.toString()
        }

        Files.write(preferencesFile, preferencesContent.toByteArray(US_ASCII))
    }

    private fun removeMod(path: Path) {
        logger.debug("Removing mod: {}", path)
        installedModVersions.remove(pathToMod.remove(path))
    }

    private fun addMod(path: Path) {
        logger.debug("Adding mod: {}", path)
        try {
            val modVersion = extractModInfo(path)
            pathToMod[path] = modVersion
            if (!installedModVersions.contains(modVersion)) {
                installedModVersions.add(modVersion)
            }
        } catch (e: ModLoadException) {
            logger.debug("Corrupt mod: $path", e)

            notificationService.addNotification(PersistentNotification(i18n.get("corruptedMods.notification", path.fileName), WARN, listOf(Action(i18n.get("corruptedMods.show")) { event -> platformService.reveal(path) })))
        }

    }

    @PreDestroy
    private fun preDestroy() {
        Optional.ofNullable(directoryWatcherThread).ifPresent(Consumer<Thread> { it.interrupt() })
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL)
        private val ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL)
    }
}
