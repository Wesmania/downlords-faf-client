package com.faforever.client.config

import com.faforever.client.FafClientApplication
import com.faforever.client.game.FaInitGenerator
import com.faforever.client.mod.ModService
import com.faforever.client.patch.FeaturedModUpdater
import com.faforever.client.patch.GameUpdater
import com.faforever.client.patch.GameUpdaterImpl
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.task.TaskService
import lombok.AllArgsConstructor
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile(FafClientApplication.PROFILE_OFFLINE)
@AllArgsConstructor
class MockFeaturedModUpdaterConfig {

    private val modService: ModService? = null
    private val applicationContext: ApplicationContext? = null
    private val taskService: TaskService? = null
    private val fafService: FafService? = null
    private val faInitGenerator: FaInitGenerator? = null
    private val featuredModUpdater: FeaturedModUpdater? = null
    private val preferencesService: PreferencesService? = null

    @Bean
    internal fun gameUpdater(): GameUpdater {
        return GameUpdaterImpl(modService, applicationContext, taskService, fafService, faInitGenerator, preferencesService)
                .addFeaturedModUpdater(featuredModUpdater)
    }
}
