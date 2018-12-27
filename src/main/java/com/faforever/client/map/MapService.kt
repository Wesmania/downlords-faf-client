package com.faforever.client.map

import com.faforever.client.config.CacheNames
import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.Vault
import com.faforever.client.fa.FaStrings
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean.Type
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.AssetService
import com.faforever.client.remote.FafService
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.CompletableTask.Priority
import com.faforever.client.task.TaskService
import com.faforever.client.theme.UiService
import com.faforever.client.util.ProgrammingError
import com.faforever.client.vault.search.SearchController.SearchConfig
import javafx.beans.property.DoubleProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.image.Image
import org.apache.maven.artifact.versioning.ComparableVersion
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Inject
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CompletableFuture

import com.faforever.client.util.LuaUtil.loadFile
import com.github.nocatch.NoCatch.noCatch
import com.google.common.net.UrlEscapers.urlFragmentEscaper
import java.lang.String.format
import java.nio.file.Files.list
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.util.stream.Collectors.toCollection


@Lazy
@Service
class MapService @Inject
constructor(private val preferencesService: PreferencesService, private val taskService: TaskService,
            private val applicationContext: ApplicationContext,
            private val fafService: FafService, private val assetService: AssetService,
            private val i18n: I18n, private val uiService: UiService, clientProperties: ClientProperties) {

    private val mapDownloadUrlFormat: String
    private val mapPreviewUrlFormat: String

    private val pathToMap: MutableMap<Path, MapBean>
    private var customMapsDirectory: Path? = null
    val installedMaps: ObservableList<MapBean>
    private val mapsByFolderName: MutableMap<String, MapBean>
    private var directoryWatcherThread: Thread? = null

    init {

        val vault = clientProperties.getVault()
        this.mapDownloadUrlFormat = vault.getMapDownloadUrlFormat()
        this.mapPreviewUrlFormat = vault.getMapPreviewUrlFormat()

        pathToMap = HashMap()
        installedMaps = FXCollections.observableArrayList()
        mapsByFolderName = HashMap()

        installedMaps.addListener({ change ->
            while (change.next()) {
                for (mapBean in change.getRemoved()) {
                    mapsByFolderName.remove(mapBean.getFolderName().toLowerCase())
                }
                for (mapBean in change.getAddedSubList()) {
                    mapsByFolderName[mapBean.getFolderName().toLowerCase()] = mapBean
                }
            }
        } as ListChangeListener<MapBean>)
    }

    @PostConstruct
    internal fun postConstruct() {
        customMapsDirectory = preferencesService.preferences!!.forgedAlliance.customMapsDirectory
        JavaFxUtil.addListener(preferencesService.preferences!!.forgedAlliance.pathProperty()) { observable -> tryLoadMaps() }
        JavaFxUtil.addListener(preferencesService.preferences!!.forgedAlliance.customMapsDirectoryProperty()) { observable -> tryLoadMaps() }
        tryLoadMaps()
    }

    private fun tryLoadMaps() {
        if (preferencesService.preferences!!.forgedAlliance.path == null || preferencesService.preferences!!.forgedAlliance.customMapsDirectory == null) {
            return
        }
        try {
            Files.createDirectories(customMapsDirectory)
            directoryWatcherThread = startDirectoryWatcher(customMapsDirectory)
        } catch (e: IOException) {
            logger.warn("Could not start map directory watcher", e)
            // TODO notify user
        }

        loadInstalledMaps()
    }

    private fun startDirectoryWatcher(mapsDirectory: Path?): Thread {
        val thread = Thread {
            noCatch {
                val watcher = mapsDirectory!!.fileSystem.newWatchService()
                this@MapService.customMapsDirectory!!.register(watcher, ENTRY_DELETE)

                try {
                    while (!Thread.interrupted()) {
                        val key = watcher.take()
                        key.pollEvents().stream()
                                .filter { event -> event.kind() === ENTRY_DELETE }
                                .forEach { event -> removeMap(mapsDirectory.resolve(event.context() as Path)) }
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

    private fun loadInstalledMaps() {
        taskService.submitTask<>(object : CompletableTask<Void>(Priority.LOW) {

            override fun call(): Void? {
                updateTitle(i18n.get("mapVault.loadingMaps"))
                val officialMapsPath = preferencesService.preferences!!.forgedAlliance.path.resolve("maps")

                try {
                    val mapPaths = ArrayList<Path>()
                    Files.list(customMapsDirectory!!).collect<List<Path>, Any>(toCollection { mapPaths })
                    Arrays.stream(OfficialMap.values())
                            .map { map -> officialMapsPath.resolve(map.name) }
                            .collect<List<Path>, Any>(toCollection { mapPaths })

                    val totalMaps = mapPaths.size.toLong()
                    var mapsRead: Long = 0
                    for (mapPath in mapPaths) {
                        updateProgress(++mapsRead, totalMaps)
                        addSkirmishMap(mapPath)
                    }
                } catch (e: IOException) {
                    logger.warn("Maps could not be read from: " + customMapsDirectory!!, e)
                }

                return null
            }
        })
    }

    private fun removeMap(path: Path) {
        installedMaps.remove(pathToMap.remove(path))
    }

    @Throws(MapLoadException::class)
    private fun addSkirmishMap(path: Path?) {
        try {
            val mapBean = readMap(path)
            pathToMap[path] = mapBean
            if (!mapsByFolderName.containsKey(mapBean.folderName) && mapBean.type == Type.SKIRMISH) {
                installedMaps.add(mapBean)
            }
        } catch (e: MapLoadException) {
            logger.warn("Map could not be read: " + path!!.fileName, e)
        }

    }


    @Throws(MapLoadException::class)
    fun readMap(mapFolder: Path?): MapBean {
        if (!Files.isDirectory(mapFolder)) {
            throw MapLoadException("Not a folder: " + mapFolder!!.toAbsolutePath())
        }

        val scenarioLuaPath = noCatch<Stream<Path>> { list(mapFolder!!) }
                .filter({ file -> file.getFileName().toString().endsWith("_scenario.lua") })
                .findFirst()
                .orElseThrow({ MapLoadException("Map folder does not contain a *_scenario.lua: " + mapFolder!!.toAbsolutePath()) })

        try {
            val luaRoot = noCatch<LuaValue>({ loadFile(scenarioLuaPath) }, MapLoadException::class.java)
            val scenarioInfo = luaRoot.get("ScenarioInfo")
            val size = scenarioInfo.get("size")

            val mapBean = MapBean()
            mapBean.folderName = mapFolder!!.fileName.toString()
            mapBean.displayName = scenarioInfo.get("name").toString()
            mapBean.description = FaStrings.removeLocalizationTag(scenarioInfo.get("description").toString())
            mapBean.type = Type.fromString(scenarioInfo.get("type").toString())
            mapBean.size = MapSize.valueOf(size.get(1).toint(), size.get(2).toint())
            mapBean.players = scenarioInfo.get("Configurations").get("standard").get("teams").get(1).get("armies").length()

            val mapVersion = scenarioInfo.get("map_version")
            if (!mapVersion.isnil()) {
                mapBean.setVersion(ComparableVersion(mapVersion.toString()))
            }

            return mapBean
        } catch (e: LuaError) {
            throw MapLoadException(e)
        }

    }

    @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result == null")
    fun loadPreview(mapName: String, previewSize: PreviewSize): Image {
        return loadPreview(getPreviewUrl(mapName, mapPreviewUrlFormat, previewSize), previewSize)
    }


    fun getMapLocallyFromName(mapFolderName: String): Optional<MapBean> {
        logger.debug("Trying to find map '{}' locally", mapFolderName)
        val installedMaps = installedMaps
        synchronized(installedMaps) {
            for (mapBean in installedMaps) {
                if (mapFolderName.equals(mapBean.folderName, ignoreCase = true)) {
                    logger.debug("Found map {} locally", mapFolderName)
                    return Optional.of(mapBean)
                }
            }
        }
        return Optional.empty()
    }


    fun isOfficialMap(mapName: String): Boolean {
        return OfficialMap.fromMapName(mapName) != null
    }


    /**
     * Returns `true` if the given map is available locally, `false` otherwise.
     */

    fun isInstalled(mapFolderName: String): Boolean {
        return mapsByFolderName.containsKey(mapFolderName.toLowerCase())
    }


    fun download(technicalMapName: String): CompletableFuture<Void> {
        val mapUrl = getDownloadUrl(technicalMapName, mapDownloadUrlFormat)
        return downloadAndInstallMap(technicalMapName, mapUrl, null, null)
    }


    fun downloadAndInstallMap(map: MapBean, progressProperty: DoubleProperty?, titleProperty: StringProperty?): CompletableFuture<Void> {
        return downloadAndInstallMap(map.folderName, map.downloadUrl, progressProperty, titleProperty)
    }


    fun getHighestRatedMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return fafService.getHighestRatedMaps(count, page)
    }


    fun getNewestMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return fafService.getNewestMaps(count, page)
    }


    fun getMostPlayedMaps(count: Int, page: Int): CompletableFuture<List<MapBean>> {
        return fafService.getMostPlayedMaps(count, page)
    }

    /**
     * Loads the preview of a map or returns a "unknown map" image.
     */

    @Cacheable(CacheNames.MAP_PREVIEW)
    fun loadPreview(map: MapBean, previewSize: PreviewSize): Image? {
        val url: URL
        when (previewSize) {
            MapService.PreviewSize.SMALL -> url = map.smallThumbnailUrl
            MapService.PreviewSize.LARGE -> url = map.largeThumbnailUrl
            else -> throw ProgrammingError("Uncovered preview size: $previewSize")
        }
        return loadPreview(url, previewSize)
    }

    @Cacheable(CacheNames.MAP_PREVIEW)
    fun loadPreview(url: URL, previewSize: PreviewSize): Image? {
        return assetService.loadAndCacheImage(url, Paths.get("maps").resolve(previewSize.folderName)
        ) { uiService.getThemeImage(UiService.UNKNOWN_MAP_IMAGE) }
    }


    fun uninstallMap(map: MapBean): CompletableFuture<Void> {
        val task = applicationContext.getBean(com.faforever.client.map.UninstallMapTask::class.java)
        task.setMap(map)
        return taskService.submitTask(task).future
    }


    fun getPathForMap(map: MapBean): Path? {
        return getPathForMap(map.folderName)
    }


    fun getPathForMap(technicalName: String): Path? {
        val path = preferencesService.preferences!!.forgedAlliance.customMapsDirectory.resolve(technicalName)
        return if (Files.notExists(path)) {
            null
        } else path
    }


    fun uploadMap(mapPath: Path, ranked: Boolean): CompletableTask<Void> {
        val mapUploadTask = applicationContext.getBean(MapUploadTask::class.java)
        mapUploadTask.setMapPath(mapPath)
        mapUploadTask.setRanked(ranked)

        return taskService.submitTask(mapUploadTask)
    }


    @CacheEvict(CacheNames.MAPS)
    fun evictCache() {
        // Nothing to see here
    }

    /**
     * Tries to find a map my its folder name, first locally then on the server.
     */

    fun findByMapFolderName(folderName: String): CompletableFuture<Optional<MapBean>> {
        val localMapFolder = getPathForMap(folderName)
        return if (localMapFolder != null && Files.exists(localMapFolder)) {
            CompletableFuture.completedFuture(Optional.of(readMap(localMapFolder)))
        } else fafService.findMapByFolderName(folderName)
    }


    fun hasPlayedMap(playerId: Int, mapVersionId: String): CompletableFuture<Boolean> {
        return fafService.getLastGameOnMap(playerId, mapVersionId)
                .thenApply(Function<Optional<Replay>, Boolean> { it.isPresent() })
    }


    @Async
    fun getFileSize(downloadUrl: URL): CompletableFuture<Int> {
        return CompletableFuture.completedFuture(noCatch<Int> {
            downloadUrl
                    .openConnection()
                    .contentLength
        })
    }


    fun findByQuery(searchConfig: SearchConfig, page: Int, count: Int): CompletableFuture<List<MapBean>> {
        return fafService.findMapsByQuery(searchConfig, page, count)
    }


    fun findMap(id: String): Optional<MapBean> {
        return fafService.findMapById(id)
    }


    fun getLadderMaps(loadMoreCount: Int, page: Int): CompletableFuture<List<MapBean>> {
        return fafService.getLadder1v1Maps(loadMoreCount, page)
    }

    private fun downloadAndInstallMap(folderName: String, downloadUrl: URL, progressProperty: DoubleProperty?, titleProperty: StringProperty?): CompletableFuture<Void> {
        val task = applicationContext.getBean(DownloadMapTask::class.java)
        task.setMapUrl(downloadUrl)
        task.setFolderName(folderName)

        progressProperty?.bind(task.progressProperty())
        titleProperty?.bind(task.titleProperty())

        return taskService.submitTask(task).future
                .thenAccept { aVoid -> noCatch { addSkirmishMap(getPathForMap(folderName)) } }
    }

    fun getOwnedMaps(playerId: Int, loadMoreCount: Int, page: Int): CompletableFuture<List<MapBean>> {
        return fafService.getOwnedMaps(playerId, loadMoreCount, page)
    }

    fun hideMapVersion(map: MapBean): CompletableFuture<Void> {
        applicationContext.getBean(this.javaClass).evictCache()
        return fafService.hideMapVersion(map)
    }

    fun unrankMapVersion(map: MapBean): CompletableFuture<Void> {
        applicationContext.getBean(this.javaClass).evictCache()
        return fafService.unrankeMapVersion(map)
    }

    @PreDestroy
    private fun preDestroy() {
        Optional.ofNullable(directoryWatcherThread).ifPresent(Consumer<Thread> { it.interrupt() })
    }

    enum class OfficialMap {
        SCMP_001, SCMP_002, SCMP_003, SCMP_004, SCMP_005, SCMP_006, SCMP_007, SCMP_008, SCMP_009, SCMP_010, SCMP_011,
        SCMP_012, SCMP_013, SCMP_014, SCMP_015, SCMP_016, SCMP_017, SCMP_018, SCMP_019, SCMP_020, SCMP_021, SCMP_022,
        SCMP_023, SCMP_024, SCMP_025, SCMP_026, SCMP_027, SCMP_028, SCMP_029, SCMP_030, SCMP_031, SCMP_032, SCMP_033,
        SCMP_034, SCMP_035, SCMP_036, SCMP_037, SCMP_038, SCMP_039, SCMP_040, X1MP_001, X1MP_002, X1MP_003, X1MP_004,
        X1MP_005, X1MP_006, X1MP_007, X1MP_008, X1MP_009, X1MP_010, X1MP_011, X1MP_012, X1MP_014, X1MP_017;


        companion object {

            private val fromString: MutableMap<String, OfficialMap>

            init {
                fromString = HashMap()
                for (officialMap in values()) {
                    fromString[officialMap.name] = officialMap
                }
            }

            fun fromMapName(mapName: String): OfficialMap? {
                return fromString[mapName.toUpperCase()]
            }
        }
    }

    enum class PreviewSize private constructor(internal var folderName: String) {
        // These must match the preview URLs
        SMALL("small"),
        LARGE("large")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private fun getDownloadUrl(mapName: String, baseUrl: String): URL {
            return noCatch<URL> { URL(format(baseUrl, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US))) }
        }

        private fun getPreviewUrl(mapName: String, baseUrl: String, previewSize: PreviewSize): URL {
            return noCatch<URL> { URL(format(baseUrl, previewSize.folderName, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US))) }
        }
    }
}
