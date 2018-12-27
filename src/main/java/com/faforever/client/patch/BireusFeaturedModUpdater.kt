package com.faforever.client.patch

import com.faforever.client.FafClientApplication
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.task.TaskService
import com.faforever.client.util.Validator
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.extern.slf4j.Slf4j
import net.brutus5000.bireus.data.Repository
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import java.io.IOException
import java.net.URL
import java.util.concurrent.CompletableFuture

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@Slf4j
class BireusFeaturedModUpdater(private val taskService: TaskService, private val applicationContext: ApplicationContext, private val objectMapper: ObjectMapper) : FeaturedModUpdater {

    override fun updateMod(featuredMod: FeaturedMod, version: Int?): CompletableFuture<PatchResult> {
        val task = applicationContext.getBean(BireusFeaturedModUpdateTask::class.java)
        task.setVersion(version)
        task.setFeaturedMod(featuredMod)

        return taskService.submitTask(task).future
    }

    override fun canUpdate(featuredMod: FeaturedMod): Boolean {
        val repoUrl = featuredMod.bireusUrl ?: return false

        try {
            val repoInfoUrl = URL(repoUrl.toString() + "/" + Repository.BIREUS_INFO_FILE)
            val map = objectMapper.readValue(repoInfoUrl, Map<*, *>::class.java)
            return Validator.isInt(map["protocol"].toString()) && map["protocol"] as Int == PROTOCOL_VERSION
        } catch (e: IOException) {
            log.warn("Error while testing {}", repoUrl, e)
            return false
        }

    }

    companion object {

        private val PROTOCOL_VERSION = 1
    }
}
