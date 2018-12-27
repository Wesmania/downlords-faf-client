package com.faforever.client

import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.main.MainController
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import com.faforever.client.ui.StageHolder
import com.faforever.client.ui.taskbar.WindowsTaskbarProgressUpdater
import javafx.application.Application
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.boot.Banner.Mode
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean

import java.util.ArrayList

@SpringBootApplication(exclude = arrayOf(JmxAutoConfiguration::class, SecurityAutoConfiguration::class))
@EnableConfigurationProperties(ClientProperties::class)
class FafClientApplication : Application() {

    private var applicationContext: ConfigurableApplicationContext? = null

    override fun init() {
        Font.loadFont(FafClientApplication::class.java.getResourceAsStream("/font/dfc-icons.ttf"), 10.0)
        JavaFxUtil.fixTooltipDuration()

        applicationContext = SpringApplicationBuilder(FafClientApplication::class.java)
                .profiles(*additionalProfiles)
                .bannerMode(Mode.OFF)
                .run(*parameters.raw.toTypedArray())
    }

    override fun start(stage: Stage) {
        StageHolder.stage = stage
        stage.initStyle(StageStyle.UNDECORATED)
        showMainWindow()
        JavaFxUtil.fixJDK8089296()

        // TODO publish event instead
        if (!applicationContext!!.getBeansOfType(WindowsTaskbarProgressUpdater::class.java).isEmpty()) {
            applicationContext!!.getBean(WindowsTaskbarProgressUpdater::class.java).initTaskBar()
        }
    }

    @Bean
    fun platformService(): PlatformService {
        return PlatformService(hostServices)
    }

    private fun showMainWindow() {
        val controller = applicationContext!!.getBean(UiService::class.java).loadFxml<MainController>("theme/main.fxml")
        controller.display()
    }

    @Throws(Exception::class)
    override fun stop() {
        applicationContext!!.close()
        super.stop()
    }

    companion object {
        val PROFILE_PROD = "prod"
        val PROFILE_TEST = "test"
        val PROFILE_LOCAL = "local"
        val PROFILE_OFFLINE = "offline"
        val PROFILE_WINDOWS = "windows"
        val PROFILE_LINUX = "linux"
        val PROFILE_MAC = "mac"

        @JvmStatic
        fun main(args: Array<String>) {
            PreferencesService.configureLogging()
            Application.launch(*args)
        }

        private val additionalProfiles: Array<String>
            get() {
                val additionalProfiles = ArrayList<String>()

                if (org.bridj.Platform.isWindows()) {
                    additionalProfiles.add(PROFILE_WINDOWS)
                } else if (org.bridj.Platform.isLinux()) {
                    additionalProfiles.add(PROFILE_LINUX)
                } else if (org.bridj.Platform.isMacOSX()) {
                    additionalProfiles.add(PROFILE_MAC)
                }
                return additionalProfiles.toTypedArray()
            }
    }
}
