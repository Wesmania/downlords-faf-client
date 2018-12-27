package com.faforever.client.update

import lombok.Value

import java.net.URL

@Value
internal class UpdateInfo {

    private val name: String? = null
    private val fileName: String? = null
    private val url: URL? = null
    private val size: Int = 0
    private val releaseNotesUrl: URL? = null

}
