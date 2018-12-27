package com.faforever.client.preferences

import com.faforever.client.config.ClientProperties
import com.faforever.client.game.Faction
import com.faforever.client.preferences.gson.ColorTypeAdapter
import com.faforever.client.preferences.gson.PathTypeAdapter
import com.faforever.client.preferences.gson.PropertyTypeAdapter
import com.faforever.client.remote.gson.FactionTypeAdapter
import com.faforever.client.update.ClientConfiguration
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import javafx.beans.property.Property
import javafx.collections.ObservableMap
import javafx.scene.paint.Color
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer
import java.lang.invoke.MethodHandles
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CompletableFuture

@Lazy
@Service
class PreferencesService(private val clientProperties: ClientProperties) {

    private val preferencesFilePath: Path
    private val gson: Gson
    /**
     * @see .storeInBackground
     */
    private val timer: Timer
    private val updateListeners: MutableCollection<WeakReference<PreferenceUpdateListener>>

    var preferences: Preferences? = null
        private set
    private var storeInBackgroundTask: TimerTask? = null

    val preferencesDirectory: Path
        get() = if (org.bridj.Platform.isWindows()) {
            Paths.get(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER)
        } else Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER)

    val fafBinDirectory: Path
        get() = fafDataDirectory.resolve("bin")

    val fafDataDirectory: Path
        get() = FAF_DATA_DIRECTORY

    val patchReposDirectory: Path
        get() = fafDataDirectory.resolve("repos")

    val corruptedReplaysDirectory: Path
        get() = replaysDirectory.resolve(CORRUPTED_REPLAYS_SUB_FOLDER)

    val replaysDirectory: Path
        get() = fafDataDirectory.resolve(REPLAYS_SUB_FOLDER)

    val cacheDirectory: Path
        get() = CACHE_DIRECTORY

    val fafLogDirectory: Path
        get() = fafDataDirectory.resolve("logs")

    val themesDirectory: Path
        get() = fafDataDirectory.resolve("themes")

    val isGamePathValid: Boolean
        get() = isGamePathValid(preferences!!.forgedAlliance.path.resolve("bin"))

    val cacheStylesheetsDirectory: Path
        get() = fafDataDirectory.resolve(CACHE_STYLESHEETS_SUB_FOLDER)

    val languagesDirectory: Path
        get() = fafDataDirectory.resolve("languages")

    val remotePreferences: CompletableFuture<ClientConfiguration>
        get() {
            val future = CompletableFuture<ClientConfiguration>()

            try {
                val url = URL(clientProperties.getClientConfigUrl())
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = clientProperties.getClientConfigConnectTimeout().toMillis()

                var clientConfiguration: ClientConfiguration
                InputStreamReader(urlConnection.inputStream, StandardCharsets.UTF_8).use { reader -> clientConfiguration = gson.fromJson(reader, ClientConfiguration::class.java) }
                future.complete(clientConfiguration)
            } catch (e: IOException) {
                future.completeExceptionally(e)
            }

            return future
        }

    init {
        updateListeners = ArrayList()
        this.preferencesFilePath = preferencesDirectory.resolve(PREFS_FILE_NAME)
        timer = Timer("PrefTimer", true)
        gson = GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(Property<*>::class.java, PropertyTypeAdapter.INSTANCE)
                .registerTypeHierarchyAdapter(Path::class.java, PathTypeAdapter.INSTANCE)
                .registerTypeAdapter(Color::class.java, ColorTypeAdapter())
                .registerTypeAdapter(Faction::class.java, FactionTypeAdapter.INSTANCE)
                .registerTypeAdapter(ObservableMap<*, *>::class.java, FactionTypeAdapter.INSTANCE)
                .create()
    }

    @PostConstruct
    @Throws(IOException::class)
    fun postConstruct() {
        if (Files.exists(preferencesFilePath)) {
            deleteFileIfEmpty()
            readExistingFile(preferencesFilePath)
        } else {
            preferences = Preferences()
        }

        val gamePrefs = preferences!!.forgedAlliance.preferencesFile
        if (Files.notExists(gamePrefs)) {
            logger.info("Initializing game preferences file: {}", gamePrefs)
            Files.createDirectories(gamePrefs.parent)
            Files.copy(javaClass.getResourceAsStream("/game.prefs"), gamePrefs)
        }
    }

    /**
     * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
     * migrations.
     */
    private fun migratePreferences(preferences: Preferences) {
        preferences.forgedAlliance.installationPath = preferences.forgedAlliance.installationPath
        storeInBackground()
    }


    /**
     * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
     */
    @Throws(IOException::class)
    private fun deleteFileIfEmpty() {
        if (Files.size(preferencesFilePath) == 0L) {
            Files.delete(preferencesFilePath)
        }
    }

    private fun readExistingFile(path: Path) {
        if (preferences != null) {
            throw IllegalStateException("Preferences have already been initialized")
        }

        try {
            Files.newBufferedReader(path, CHARSET).use { reader ->
                logger.debug("Reading preferences file {}", preferencesFilePath.toAbsolutePath())
                preferences = gson.fromJson(reader, Preferences::class.java)
            }
        } catch (e: IOException) {
            logger.warn("Preferences file " + path.toAbsolutePath() + " could not be read", e)
        }

        migratePreferences(preferences!!)
    }

    fun store() {
        val parent = preferencesFilePath.parent
        try {
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
        } catch (e: IOException) {
            logger.warn("Could not create directory " + parent.toAbsolutePath(), e)
            return
        }

        try {
            Files.newBufferedWriter(preferencesFilePath, CHARSET).use { writer ->
                logger.debug("Writing preferences file {}", preferencesFilePath.toAbsolutePath())
                gson.toJson(preferences, writer)
            }
        } catch (e: IOException) {
            logger.warn("Preferences file " + preferencesFilePath.toAbsolutePath() + " could not be written", e)
        }

    }

    /**
     * Stores the preferences in background, with a delay of [.STORE_DELAY]. Each subsequent call to this method
     * during that delay causes the delay to be reset. This ensures that the prefs file is written only once if multiple
     * calls occur within a short time.
     */
    fun storeInBackground() {
        if (storeInBackgroundTask != null) {
            storeInBackgroundTask!!.cancel()
        }

        storeInBackgroundTask = object : TimerTask() {
            override fun run() {
                store()
                val toBeRemoved = ArrayList<WeakReference<PreferenceUpdateListener>>()
                for (updateListener in updateListeners) {
                    val preferenceUpdateListener = updateListener.get()
                    if (preferenceUpdateListener == null) {
                        toBeRemoved.add(updateListener)
                        continue
                    }
                    preferenceUpdateListener.onPreferencesUpdated(preferences)
                }

                for (preferenceUpdateListenerWeakReference in toBeRemoved) {
                    updateListeners.remove(preferenceUpdateListenerWeakReference)
                }
            }
        }

        timer.schedule(storeInBackgroundTask!!, STORE_DELAY)
    }

    /**
     * Adds a listener to be notified whenever the preferences have been updated (that is, stored to file).
     */
    fun addUpdateListener(listener: WeakReference<PreferenceUpdateListener>) {
        updateListeners.add(listener)
    }

    fun isGamePathValid(binPath: Path?): Boolean {
        return binPath != null && (Files.isRegularFile(binPath.resolve(FORGED_ALLIANCE_EXE)) || Files.isRegularFile(binPath.resolve(SUPREME_COMMANDER_EXE)))
    }

    companion object {

        val SUPREME_COMMANDER_EXE = "SupremeCommander.exe"
        val FORGED_ALLIANCE_EXE = "ForgedAlliance.exe"

        /**
         * Points to the FAF data directory where log files, config files and others are held. The returned value varies
         * depending on the operating system.
         */
        private val FAF_DATA_DIRECTORY: Path
        private val logger: Logger
        private val STORE_DELAY: Long = 1000
        private val CHARSET = StandardCharsets.UTF_8
        private val PREFS_FILE_NAME = "client.prefs"
        private val APP_DATA_SUB_FOLDER = "Forged Alliance Forever"
        private val USER_HOME_SUB_FOLDER = ".faforever"
        private val REPLAYS_SUB_FOLDER = "replays"
        private val CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt"
        private val CACHE_SUB_FOLDER = "cache"
        private val CACHE_STYLESHEETS_SUB_FOLDER = Paths.get(CACHE_SUB_FOLDER, "stylesheets").toString()
        private val CACHE_DIRECTORY: Path

        init {
            if (org.bridj.Platform.isWindows()) {
                FAF_DATA_DIRECTORY = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever")
            } else {
                FAF_DATA_DIRECTORY = Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER)
            }
            CACHE_DIRECTORY = FAF_DATA_DIRECTORY.resolve(CACHE_SUB_FOLDER)

            System.setProperty("logging.file", PreferencesService.FAF_DATA_DIRECTORY
                    .resolve("logs")
                    .resolve("downlords-faf-client.log")
                    .toString())

            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()

            logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
            logger.debug("Logger initialized")
        }

        fun configureLogging() {
            // Calling this method causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
        }
    }
}
