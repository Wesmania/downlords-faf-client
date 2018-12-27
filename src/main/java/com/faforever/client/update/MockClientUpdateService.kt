package com.faforever.client.update

import com.faforever.client.FafClientApplication
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service


@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
class MockClientUpdateService : ClientUpdateService {

    override val currentVersion: ComparableVersion
        get() = ComparableVersion("dev")

    override fun checkForUpdateInBackground() {

    }
}
