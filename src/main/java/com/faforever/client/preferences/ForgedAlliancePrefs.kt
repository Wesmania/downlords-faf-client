package com.faforever.client.preferences

import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.nio.file.Path
import java.nio.file.Paths

class ForgedAlliancePrefs {


    @Deprecated("use {@code installationPath} instead.")
    private val path: ObjectProperty<Path>
    private val installationPath: ObjectProperty<Path>
    private val customMapsDirectory: ObjectProperty<Path>
    private val preferencesFile: ObjectProperty<Path>
    private val officialMapsDirectory: ObjectProperty<Path>
    private val modsDirectory: ObjectProperty<Path>
    private val port: IntegerProperty
    private val autoDownloadMaps: BooleanProperty

    /**
     * String format to use when building the launch command. Takes exact one parameter; the executable path.
     *
     *
     * Example:
     * <pre>wine "%s"</pre>
     * Results in:
     * <pre>wine "C:\Game\ForgedAlliance.exe"</pre>
     *
     */
    private val executableDecorator: StringProperty
    private val executionDirectory: ObjectProperty<Path>

    init {
        port = SimpleIntegerProperty(6112)
        path = SimpleObjectProperty()
        installationPath = SimpleObjectProperty()
        customMapsDirectory = SimpleObjectProperty(GPG_FA_PATH.resolve("Maps"))
        officialMapsDirectory = SimpleObjectProperty(STEAM_FA_PATH.resolve("Maps"))
        modsDirectory = SimpleObjectProperty(GPG_FA_PATH.resolve("Mods"))
        preferencesFile = SimpleObjectProperty(LOCAL_FA_DATA_PATH.resolve("Game.prefs"))
        autoDownloadMaps = SimpleBooleanProperty(true)
        executableDecorator = SimpleStringProperty("\"%s\"")
        executionDirectory = SimpleObjectProperty()
    }

    fun getPreferencesFile(): Path {
        return preferencesFile.get()
    }

    fun setPreferencesFile(preferencesFile: Path) {
        this.preferencesFile.set(preferencesFile)
    }

    fun preferencesFileProperty(): ObjectProperty<Path> {
        return preferencesFile
    }

    fun getOfficialMapsDirectory(): Path {
        return officialMapsDirectory.get()
    }

    fun setOfficialMapsDirectory(officialMapsDirectory: Path) {
        this.officialMapsDirectory.set(officialMapsDirectory)
    }


    @Deprecated("use {@code installationPath} instead.")
    fun getPath(): Path {
        return path.get()
    }


    @Deprecated("use {@code installationPath} instead.")
    fun setPath(path: Path) {
        this.path.set(path)
        this.installationPath.set(path)
    }

    fun pathProperty(): ObjectProperty<Path> {
        return path
    }

    fun getPort(): Int {
        return port.get()
    }

    fun setPort(port: Int) {
        this.port.set(port)
    }

    fun portProperty(): IntegerProperty {
        return port
    }

    fun getAutoDownloadMaps(): Boolean {
        return autoDownloadMaps.get()
    }

    fun setAutoDownloadMaps(autoDownloadMaps: Boolean) {
        this.autoDownloadMaps.set(autoDownloadMaps)
    }

    fun autoDownloadMapsProperty(): BooleanProperty {
        return autoDownloadMaps
    }

    fun getModsDirectory(): Path {
        return modsDirectory.get()
    }

    fun setModsDirectory(modsDirectory: Path) {
        this.modsDirectory.set(modsDirectory)
    }

    fun modsDirectoryProperty(): ObjectProperty<Path> {
        return modsDirectory
    }

    fun getCustomMapsDirectory(): Path {
        return customMapsDirectory.get()
    }

    fun setCustomMapsDirectory(customMapsDirectory: Path) {
        this.customMapsDirectory.set(customMapsDirectory)
    }

    fun customMapsDirectoryProperty(): ObjectProperty<Path> {
        return customMapsDirectory
    }

    fun officialMapsDirectoryProperty(): ObjectProperty<Path> {
        return officialMapsDirectory
    }

    fun getExecutableDecorator(): String {
        return executableDecorator.get()
    }

    fun setExecutableDecorator(executableDecorator: String) {
        this.executableDecorator.set(executableDecorator)
    }

    fun executableDecoratorProperty(): StringProperty {
        return executableDecorator
    }

    fun getExecutionDirectory(): Path {
        return executionDirectory.get()
    }

    fun setExecutionDirectory(executionDirectory: Path) {
        this.executionDirectory.set(executionDirectory)
    }

    fun executionDirectoryProperty(): ObjectProperty<Path> {
        return executionDirectory
    }

    fun getInstallationPath(): Path {
        return installationPath.get()
    }

    fun setInstallationPath(installationPath: Path) {
        this.installationPath.set(installationPath)
    }

    fun installationPathProperty(): ObjectProperty<Path> {
        return installationPath
    }

    companion object {

        val GPG_FA_PATH: Path
        val STEAM_FA_PATH: Path
        val LOCAL_FA_DATA_PATH: Path
        val INIT_FILE_NAME = "init.lua"

        init {
            if (org.bridj.Platform.isWindows()) {
                GPG_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance")
                //If steam is every swapped to a 64x client, needs to be updated to proper directory or handling must be put in place.
                STEAM_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAM_FILESX86), "Steam", "SteamApps", "common", "Supreme Commander Forged Alliance")
                LOCAL_FA_DATA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA), "Gas Powered Games", "Supreme Commander Forged Alliance")
            } else {
                val userHome = System.getProperty("user.home")
                GPG_FA_PATH = Paths.get(userHome, "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance")
                STEAM_FA_PATH = Paths.get(".")
                LOCAL_FA_DATA_PATH = Paths.get(userHome, ".wine", "drive_c", "users", System.getProperty("user.name"), "Application Data", "Gas Powered Games", "Supreme Commander Forged Alliance")
            }
        }
    }
}
