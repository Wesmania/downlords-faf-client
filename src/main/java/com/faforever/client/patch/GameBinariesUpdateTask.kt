package com.faforever.client.patch

import com.faforever.client.task.PrioritizedCompletableTask
import org.apache.maven.artifact.versioning.ComparableVersion

interface GameBinariesUpdateTask : PrioritizedCompletableTask<Void> {
    fun setVersion(version: ComparableVersion)
}
