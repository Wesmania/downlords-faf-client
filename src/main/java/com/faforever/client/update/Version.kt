package com.faforever.client.update

object Version {
    val VERSION: String

    init {
        val version = Version::class.java.getPackage().implementationVersion
        VERSION = version ?: "snapshot"
    }
}
