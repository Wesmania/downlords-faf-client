package com.faforever.client.chat

import com.faforever.client.config.CacheNames
import com.faforever.client.i18n.I18n
import com.faforever.client.theme.UiService
import com.google.common.net.MediaType
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import lombok.SneakyThrows
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher
import java.util.regex.Pattern

import com.faforever.commons.io.Bytes.formatSize


@Lazy
@Component
// TODO reintroduce once it's working better
class UrlPreviewResolverImpl @Inject
constructor(private val uiService: UiService, private val i18n: I18n) : UrlPreviewResolver {

    @Cacheable(CacheNames.URL_PREVIEW)
    @Async
    @SneakyThrows
    override fun resolvePreview(urlString: String): CompletableFuture<Optional<UrlPreviewResolver.Preview>> {
        val guessedUrl = guessUrl(urlString)

        val url = URL(guessedUrl)

        val protocol = url.protocol

        if ("http" != protocol && "https" != protocol) {
            // TODO log unhandled protocol
            return CompletableFuture.completedFuture(Optional.empty<Preview>())
        }

        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true

        val contentLength = connection.contentLengthLong
        val contentType = connection.contentType

        val root = uiService.loadFxml<Controller<*>>("theme/image_preview.fxml")
        val imageView = root.lookup("#imageView") as ImageView

        if (MediaType.JPEG.toString() == contentType || MediaType.PNG.toString() == contentType) {
            imageView.image = Image(guessedUrl)
        }

        val description = i18n.get("urlPreviewDescription", contentType, formatSize(contentLength, i18n.userSpecificLocale))

        return CompletableFuture.completedFuture(Optional.of<Preview>(UrlPreviewResolver.Preview(imageView, description)))
    }

    private fun guessUrl(urlString: String): String {
        val matcher = IMGUR_PATTERN.matcher(urlString)
        return if (matcher.find()) {
            guessImgurUrl(urlString, matcher)
        } else urlString

    }

    private fun guessImgurUrl(urlString: String, matcher: Matcher): String {
        val imageId = matcher.group(1)

        val imgurJpgUrl = String.format(IMGUR_JPG, imageId)
        if (testUrl(imgurJpgUrl)) {
            return imgurJpgUrl
        }

        val imgurPngUrl = String.format(IMGUR_PNG, imageId)
        if (testUrl(imgurPngUrl)) {
            return imgurJpgUrl
        }

        val imgurGifUrl = String.format(IMGUR_GIF, imageId)
        return if (testUrl(imgurGifUrl)) {
            imgurGifUrl
        } else urlString

    }

    companion object {

        private val IMGUR_PATTERN = Pattern.compile("https?://imgur\\.com/gallery/(\\w+)")
        private val IMGUR_JPG = "http://i.imgur.com/%s.jpg"
        private val IMGUR_PNG = "http://i.imgur.com/%s.png"
        private val IMGUR_GIF = "http://i.imgur.com/%s.gif"

        private fun testUrl(urlString: String): Boolean {
            try {
                return (URL(urlString).openConnection() as HttpURLConnection).responseCode == HttpURLConnection.HTTP_OK
            } catch (e: IOException) {
                return false
            }

        }
    }
}
