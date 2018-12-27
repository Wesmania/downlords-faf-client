package com.faforever.client.uploader.imgur

import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.Imgur.Upload
import com.faforever.client.i18n.I18n
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.ResourceLocks
import com.faforever.commons.io.ByteCopier
import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import javax.inject.Inject
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.faforever.commons.io.Bytes.formatSize


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ImgurUploadTask @Inject
constructor(private val i18n: I18n, private val clientProperties: ClientProperties) : CompletableTask<String>(CompletableTask.Priority.HIGH) {

    private val gson: Gson

    private var image: Image? = null
    private var maxUploadSize: Int = 0
    private var baseUrl: String? = null
    private var clientId: String? = null

    init {
        gson = GsonBuilder().create()
    }

    @PostConstruct
    internal fun postConstruct() {
        updateTitle(i18n.get("chat.imageUploadTask.title"))
        val uploadProperties = clientProperties.getImgur().getUpload()
        maxUploadSize = uploadProperties.getMaxSize()
        baseUrl = uploadProperties.getBaseUrl()
        clientId = uploadProperties.getClientId()
    }

    @Throws(Exception::class)
    override fun call(): String? {
        val byteArrayOutputStream = ByteArrayOutputStream()

        val bufferedImage = SwingFXUtils.fromFXImage(image!!, null)
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream)

        if (byteArrayOutputStream.size() > maxUploadSize) {
            throw IllegalArgumentException("Image exceeds max upload size of " + formatSize(maxUploadSize.toLong(), i18n.userSpecificLocale))
        }

        val dataImage = BaseEncoding.base64().encode(byteArrayOutputStream.toByteArray())
        val data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(dataImage, "UTF-8")

        val url = URL(baseUrl!!)
        val urlConnection = url.openConnection() as HttpURLConnection

        urlConnection.doOutput = true
        urlConnection.doInput = true
        urlConnection.requestMethod = "POST"
        urlConnection.setRequestProperty("Authorization", "Client-ID " + clientId!!)
        urlConnection.requestMethod = "POST"
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        urlConnection.connect()

        ResourceLocks.acquireUploadLock()
        try {
            urlConnection.outputStream.use { outputStream ->
                val bytes = data.toByteArray(StandardCharsets.UTF_8)
                ByteCopier.from(ByteArrayInputStream(bytes))
                        .to(outputStream)
                        .totalBytes(bytes.size.toLong())
                        .listener(ByteCountListener { l, l1 -> this.updateProgress(l, l1) })
                        .copy()
            }
        } finally {
            ResourceLocks.freeUploadLock()
        }

        val stringBuilder = StringBuilder()

        BufferedReader(InputStreamReader(urlConnection.inputStream)).use { reader ->
            var line: String
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n")
            }
        }

        val imgurRestResponse = gson.fromJson(stringBuilder.toString(), ImgurRestResponse::class.java)

        if (!imgurRestResponse.success) {
            throw RuntimeException("Image upload failed, status code: " + imgurRestResponse.status)
        }

        return imgurRestResponse.data!!.link
    }

    fun setImage(image: Image) {
        this.image = image
    }
}
