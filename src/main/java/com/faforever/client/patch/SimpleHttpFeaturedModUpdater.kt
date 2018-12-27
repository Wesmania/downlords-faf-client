package com.faforever.client.patch

import com.faforever.client.FafClientApplication
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.task.TaskService
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.concurrent.CompletableFuture


@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
class SimpleHttpFeaturedModUpdater @Inject
constructor(private val taskService: TaskService, private val applicationContext: ApplicationContext) : FeaturedModUpdater {

    override fun updateMod(featuredMod: FeaturedMod, version: Int?): CompletableFuture<PatchResult> {
        val task = applicationContext.getBean(SimpleHttpFeaturedModUpdaterTask::class.java)
        task.setVersion(version)
        task.setFeaturedMod(featuredMod)

        return taskService.submitTask(task).future
    }

    override fun canUpdate(featuredMod: FeaturedMod): Boolean {
        return true
    }
}
