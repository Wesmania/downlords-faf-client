package com.faforever.client.mod

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
class UninstallModTask @Inject
constructor(private val modService: ModService) : CompletableTask<Void>(CompletableTask.Priority.LOW) {

    private var modVersion: ModVersion? = null

    fun setModVersion(modVersion: ModVersion) {
        this.modVersion = modVersion
    }

    @Throws(Exception::class)
    public override fun call(): Void? {
        Objects.requireNonNull<ModVersion>(modVersion, "modVersion has not been set")

        logger.info("Uninstalling modVersion '{}' ({})", modVersion!!.displayName, modVersion!!.uid)
        val modPath = modService.getPathForMod(modVersion)

        FileUtils.deleteRecursively(modPath)

        return null
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
