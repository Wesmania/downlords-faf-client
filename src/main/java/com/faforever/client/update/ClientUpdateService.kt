package com.faforever.client.update


import org.apache.maven.artifact.versioning.ComparableVersion

interface ClientUpdateService {

    val currentVersion: ComparableVersion

    /**
     * Returns information about an available update. Returns `null` if no update is available.
     */
    fun checkForUpdateInBackground()
}
