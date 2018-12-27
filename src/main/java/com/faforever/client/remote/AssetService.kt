package com.faforever.client.remote

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.preferences.PreferencesService
import javafx.scene.image.Image
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

import com.github.nocatch.NoCatch.noCatch


@Lazy
@Service
class AssetService @Inject
constructor(private val preferencesService: PreferencesService) {

    @JvmOverloads
    fun loadAndCacheImage(url: URL?, cacheSubFolder: Path, defaultSupplier: Supplier<Image>?, width: Int = 0, height: Int = 0): Image? {
        if (url == null) {
            return defaultSupplier?.get()
        }

        val urlString = url.toString()
        val filename = urlString.substring(urlString.lastIndexOf('/') + 1)
        val cachePath = preferencesService.cacheDirectory.resolve(cacheSubFolder).resolve(filename)
        if (Files.exists(cachePath)) {
            logger.debug("Using cached image: {}", cachePath)
            return Image(noCatch<String> { cachePath.toUri().toURL().toExternalForm() }, width.toDouble(), height.toDouble(), true, true)
        }

        logger.debug("Fetching image {}", url)

        val image = Image(url.toString(), width.toDouble(), height.toDouble(), true, true, true)
        JavaFxUtil.persistImage(image, cachePath, filename.substring(filename.lastIndexOf('.') + 1))
        return image
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
