package com.faforever.client.patch

import com.faforever.commons.io.ByteCountListener
import com.google.common.io.Resources
import net.brutus5000.bireus.service.DownloadException
import net.brutus5000.bireus.service.DownloadService

import java.io.IOException
import java.net.URL
import java.nio.file.Path

class BireusDownloadServiceAdapter(private val downloadService: com.faforever.client.io.DownloadService, private val progressListener: ByteCountListener) : DownloadService {

    @Throws(DownloadException::class)
    override fun download(url: URL, targetFile: Path) {
        try {
            downloadService.downloadFile(url, targetFile, progressListener)
        } catch (e: IOException) {
            throw DownloadException(e, url)
        }

    }

    @Throws(DownloadException::class)
    override fun read(url: URL): ByteArray {
        try {
            return Resources.toByteArray(url)
        } catch (e: IOException) {
            throw DownloadException(e, url)
        }

    }
}
