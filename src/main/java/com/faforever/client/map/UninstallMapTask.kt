package com.faforever.client.map

import com.faforever.client.io.FileUtils
import com.faforever.client.task.CompletableTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.Objects

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class UninstallMapTask @Inject
constructor(private val mapService: MapService) : CompletableTask<Void>(CompletableTask.Priority.LOW) {

    private var map: MapBean? = null

    fun setMap(map: MapBean) {
        this.map = map
    }

    @Throws(Exception::class)
    public override fun call(): Void? {
        Objects.requireNonNull<MapBean>(map, "map has not been set")

        logger.info("Uninstalling map '{}' ({})", map!!.folderName, map!!.id)
        val mapPath = mapService.getPathForMap(map)

        FileUtils.deleteRecursively(mapPath)

        return null
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
