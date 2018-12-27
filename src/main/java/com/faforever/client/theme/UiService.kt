package com.faforever.client.theme

import ch.micheljung.fxborderlessscene.borderless.BorderlessScene
import com.faforever.client.config.CacheNames
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.github.nocatch.NoCatch.NoCatchRunnable
import com.jfoenix.assets.JFoenixResources
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialog.DialogTransition
import com.jfoenix.controls.JFXDialogLayout
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import lombok.SneakyThrows
import org.apache.commons.compress.utils.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.MessageSourceResourceBundle
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.lang.invoke.MethodHandles
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.net.URL
import java.nio.file.ClosedWatchServiceException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Objects
import java.util.Properties
import java.util.concurrent.ThreadPoolExecutor

import com.faforever.client.io.FileUtils.deleteRecursively
import com.faforever.client.preferences.Preferences.DEFAULT_THEME_NAME
import com.github.nocatch.NoCatch.noCatch
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY


@Lazy
@Service
class UiService(private val preferencesService: PreferencesService, private val threadPoolExecutor: ThreadPoolExecutor,
                private val cacheManager: CacheManager, private val messageSource: MessageSource, private val applicationContext: ApplicationContext,
                private val i18n: I18n) {
    private val scenes: MutableSet<Scene>
    private val webViews: MutableSet<WeakReference<WebView>>

    private var watchService: WatchService? = null
    private val themesByFolderName: ObservableMap<String, Theme>
    private val folderNamesByTheme: MutableMap<Theme, String>
    private val watchKeys: MutableMap<Path, WatchKey>
    private val currentTheme: ObjectProperty<Theme>
    private var currentTempStyleSheet: Path? = null
    private var resources: MessageSourceResourceBundle? = null

    private val sceneStyleSheet: String
        get() = getThemeFile(STYLE_CSS)


    val stylesheets: Array<String>
        get() = arrayOf(JFoenixResources.load("css/jfoenix-fonts.css").toExternalForm(), JFoenixResources.load("css/jfoenix-design.css").toExternalForm(), getThemeFile("theme/jfoenix.css"), sceneStyleSheet)


    val availableThemes: Collection<Theme>
        get() = ArrayList(themesByFolderName.values)

    private val webViewStyleSheet: String
        get() = getThemeFileUrl(WEBVIEW_CSS_FILE).toString()

    init {

        scenes = Collections.synchronizedSet(HashSet())
        webViews = HashSet()
        watchKeys = HashMap()
        currentTheme = SimpleObjectProperty(DEFAULT_THEME)
        folderNamesByTheme = HashMap()
        themesByFolderName = FXCollections.observableHashMap()
        themesByFolderName.addListener({ change ->
            if (change.wasRemoved()) {
                folderNamesByTheme.remove(change.getValueRemoved())
            }
            if (change.wasAdded()) {
                folderNamesByTheme[change.getValueAdded()] = change.getKey()
            }
        } as MapChangeListener<String, Theme>)
    }

    @PostConstruct
    @Throws(IOException::class)
    internal fun postConstruct() {
        resources = MessageSourceResourceBundle(messageSource, i18n.userSpecificLocale!!)
        val themesDirectory = preferencesService.themesDirectory
        startWatchService(themesDirectory)
        deleteStylesheetsCacheDirectory()
        loadThemes()

        val storedTheme = preferencesService.preferences!!.themeName
        setTheme(themesByFolderName[storedTheme])

        loadWebViewsStyleSheet(webViewStyleSheet)
    }

    @Throws(IOException::class)
    private fun deleteStylesheetsCacheDirectory() {
        val cacheStylesheetsDirectory = preferencesService.cacheStylesheetsDirectory
        if (Files.exists(cacheStylesheetsDirectory)) {
            deleteRecursively(cacheStylesheetsDirectory)
        }
    }

    @Throws(IOException::class)
    private fun startWatchService(themesDirectory: Path) {
        watchService = themesDirectory.fileSystem.newWatchService()
        threadPoolExecutor.execute {
            try {
                while (!Thread.interrupted()) {
                    val key = watchService!!.take()
                    onWatchEvent(key)
                    key.reset()
                }
            } catch (e: InterruptedException) {
                logger.debug("Watcher service terminated")
            } catch (e: ClosedWatchServiceException) {
                logger.debug("Watcher service terminated")
            }
        }
    }

    private fun addThemeDirectory(path: Path) {
        val metadataFile = path.resolve(METADATA_FILE_NAME)
        if (Files.notExists(metadataFile)) {
            return
        }

        try {
            Files.newBufferedReader(metadataFile).use { reader ->
                val folderName = path.fileName.toString()
                themesByFolderName.put(folderName, readTheme(reader))
            }
        } catch (e: IOException) {
            logger.warn("Theme could not be read: " + metadataFile.toAbsolutePath(), e)
        }

    }

    @Throws(IOException::class)
    private fun readTheme(reader: Reader): Theme {
        val properties = Properties()
        properties.load(reader)
        return Theme.fromProperties(properties)
    }

    @PreDestroy
    @Throws(IOException::class)
    internal fun preDestroy() {
        IOUtils.closeQuietly(watchService)
        deleteStylesheetsCacheDirectory()
    }

    private fun stopWatchingTheme(theme: Theme) {
        val path = getThemeDirectory(theme)
        if (watchKeys.containsKey(path)) {
            watchKeys.remove(path).cancel()
        }
    }

    /**
     * Watches all contents in the specified theme for changes and reloads the theme if a change is detected.
     */
    private fun watchTheme(theme: Theme) {
        val themePath = getThemeDirectory(theme)
        logger.debug("Watching theme directory for changes: {}", themePath.toAbsolutePath())
        noCatch<Path> { Files.walkFileTree(themePath, DirectoryVisitor { path -> watchDirectory(themePath, watchService) }) }
    }

    private fun onWatchEvent(key: WatchKey) {
        for (watchEvent in key.pollEvents()) {
            val path = watchEvent.context() as Path
            if (watchEvent.kind() === ENTRY_CREATE && Files.isDirectory(path)) {
                watchDirectory(path, watchService)
            } else if (watchEvent.kind() === ENTRY_DELETE && Files.isDirectory(path)) {
                watchKeys.remove(path)
            }
        }

        reloadStylesheet()
    }

    private fun watchDirectory(directory: Path, watchService: WatchService?) {
        if (watchKeys.containsKey(directory)) {
            return
        }
        logger.debug("Watching directory: {}", directory.toAbsolutePath())
        noCatch<WatchKey> { watchKeys.put(directory, directory.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)) }
    }

    private fun reloadStylesheet() {
        val styleSheets = stylesheets

        logger.debug("Changes detected, reloading stylesheets: {}", *styleSheets as Array<Any>)
        scenes.forEach { scene -> setSceneStyleSheet(scene, styleSheets) }
        loadWebViewsStyleSheet(webViewStyleSheet)
    }

    private fun setSceneStyleSheet(scene: Scene, styleSheets: Array<String>) {
        Platform.runLater { scene.stylesheets.setAll(*styleSheets) }
    }


    fun getThemeFile(relativeFile: String): String {
        val strippedRelativeFile = relativeFile.replace("theme/", "")
        val externalFile = getThemeDirectory(currentTheme.get()).resolve(strippedRelativeFile)
        return if (Files.notExists(externalFile)) {
            noCatch<String> { ClassPathResource("/$relativeFile").url.toString() }
        } else noCatch<String> { externalFile.toUri().toURL().toString() }
    }

    /**
     * Loads an image from the current theme.
     */
    @Cacheable(CacheNames.THEME_IMAGES)
    fun getThemeImage(relativeImage: String): Image {
        return Image(getThemeFile(relativeImage), true)
    }


    fun getThemeFileUrl(relativeFile: String): URL {
        val themeFile = getThemeFile(relativeFile)
        return if (themeFile.startsWith("file:") || themeFile.startsWith("jar:")) {
            noCatch<URL> { URL(themeFile) }
        } else noCatch<URL> { ClassPathResource(getThemeFile(relativeFile)).url }
    }


    fun setTheme(theme: Theme) {
        stopWatchingTheme(theme)

        if (theme === DEFAULT_THEME) {
            preferencesService.preferences!!.themeName = DEFAULT_THEME_NAME
        } else {
            watchTheme(theme)
            preferencesService.preferences!!.themeName = getThemeDirectory(theme).fileName.toString()
        }
        preferencesService.storeInBackground()
        reloadStylesheet()
        currentTheme.set(theme)
        cacheManager.getCache(CacheNames.THEME_IMAGES)!!.clear()
    }

    /**
     * Unregisters a scene so it's no longer updated when the theme (or its CSS) changes.
     */
    private fun unregisterScene(scene: Scene) {
        scenes.remove(scene)
    }

    /**
     * Registers a scene against the theme service so it can be updated whenever the theme (or its CSS) changes.
     */
    private fun registerScene(scene: Scene) {
        scenes.add(scene)

        JavaFxUtil.addListener<Window>(scene.windowProperty()) { windowProperty, oldWindow, newWindow ->
            if (oldWindow != null) {
                throw UnsupportedOperationException("Not supposed to happen")
            }
            if (newWindow != null) {
                JavaFxUtil.addListener<Boolean>(newWindow!!.showingProperty()) { observable, oldValue, newValue ->
                    if (!newValue) {
                        unregisterScene(scene)
                    } else {
                        registerScene(scene)
                    }
                }
            }
        }
        scene.stylesheets.setAll(*stylesheets)
    }

    /**
     * Registers a WebView against the theme service so it can be updated whenever the theme changes.
     */
    fun registerWebView(webView: WebView) {
        webViews.add(WeakReference(webView))
        webView.engine.userStyleSheetLocation = webViewStyleSheet
    }

    fun loadThemes() {
        themesByFolderName.clear()
        themesByFolderName[DEFAULT_THEME_NAME] = DEFAULT_THEME
        noCatch {
            Files.createDirectories(preferencesService.themesDirectory)
            Files.newDirectoryStream(preferencesService.themesDirectory).use { directoryStream -> directoryStream.forEach(Consumer<Path> { this.addThemeDirectory(it) }) }
        }
    }


    fun currentThemeProperty(): ReadOnlyObjectProperty<Theme> {
        return currentTheme
    }

    /**
     * Loads an FXML file and returns its controller instance. The controller instance is retrieved from the application
     * context, so its scope (which should always be "prototype") depends on the bean definition.
     */
    fun <T : Controller<*>> loadFxml(relativePath: String): T {
        val loader = FXMLLoader()
        loader.controllerFactory = Callback<Class<*>, Any> { applicationContext.getBean(it) }
        loader.location = getThemeFileUrl(relativePath)
        loader.resources = resources
        noCatch { loader.load() }
        return loader.getController()
    }

    private fun getThemeDirectory(theme: Theme): Path {
        return preferencesService.themesDirectory.resolve(folderNamesByTheme[theme])
    }

    @SneakyThrows
    private fun loadWebViewsStyleSheet(styleSheetUrl: String) {
        // Always copy to a new file since WebView locks the loaded one
        val stylesheetsCacheDirectory = preferencesService.cacheStylesheetsDirectory

        Files.createDirectories(stylesheetsCacheDirectory)

        val newTempStyleSheet = Files.createTempFile(stylesheetsCacheDirectory, "style-webview", ".css")

        URL(styleSheetUrl).openStream().use { inputStream -> Files.copy(inputStream, newTempStyleSheet, StandardCopyOption.REPLACE_EXISTING) }
        if (currentTempStyleSheet != null) {
            Files.delete(currentTempStyleSheet!!)
        }
        currentTempStyleSheet = newTempStyleSheet

        webViews.removeIf { reference -> reference.get() != null }
        webViews.stream()
                .map<WebView>(Function<WeakReference<WebView>, WebView> { it.get() })
                .filter(Predicate<WebView> { Objects.nonNull(it) })
                .forEach { webView -> Platform.runLater { webView.engine.userStyleSheetLocation = noCatch<URL> { currentTempStyleSheet!!.toUri().toURL() }.toString() } }
        logger.debug("{} created and applied to all web views", newTempStyleSheet.fileName)
    }

    fun createScene(stage: Stage, mainRoot: Parent): BorderlessScene {
        val scene = BorderlessScene(stage, mainRoot, 0.0, 0.0)
        scene.setMoveControl(mainRoot)
        registerScene(scene)
        return scene
    }

    fun showInDialog(parent: StackPane, content: Node, title: String): JFXDialog {
        val dialogLayout = JFXDialogLayout()
        dialogLayout.setHeading(Label(title))
        dialogLayout.setBody(content)

        val dialog = JFXDialog()
        dialog.content = dialogLayout
        dialog.transitionType = DialogTransition.TOP

        parent.setOnKeyPressed { event ->
            if (event.code == KeyCode.ESCAPE) {
                dialog.close()
            }
        }

        dialog.show(parent)
        return dialog
    }

    companion object {

        val UNKNOWN_MAP_IMAGE = "theme/images/unknown_map.png"
        //TODO: Create Images for News Categories
        val SERVER_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val LADDER_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val TOURNAMENT_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val FA_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val LOBBY_UPDATE_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val BALANCE_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val WEBSITE_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val CAST_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val PODCAST_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val FEATURED_MOD_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val DEVELOPMENT_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val DEFAULT_NEWS_IMAGE = "theme/images/news_fallback.jpg"
        val STYLE_CSS = "theme/style.css"
        val WEBVIEW_CSS_FILE = "theme/style-webview.css"
        val DEFAULT_ACHIEVEMENT_IMAGE = "theme/images/default_achievement.png"
        val MENTION_SOUND = "theme/sounds/mention.mp3"
        val CSS_CLASS_ICON = "icon"
        val LADDER_1V1_IMAGE = "theme/images/ranked1v1_notification.png"
        val CHAT_CONTAINER = "theme/chat/chat_container.html"
        val CHAT_SECTION_EXTENDED = "theme/chat/extended/chat_section.html"
        val CHAT_SECTION_COMPACT = "theme/chat/compact/chat_section.html"
        val CHAT_TEXT_EXTENDED = "theme/chat/extended/chat_text.html"
        val CHAT_TEXT_COMPACT = "theme/chat/compact/chat_text.html"

        var DEFAULT_THEME: Theme = object : Theme() {
            init {
                author = "Downlord"
                compatibilityVersion = 1
                displayName = "Default"
                themeVersion = "1.0"
            }
        }

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        /**
         * This value needs to be updated whenever theme-breaking changes were made to the client.
         */
        private val THEME_VERSION = 1
        private val METADATA_FILE_NAME = "theme.properties"
    }
}
