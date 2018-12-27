package com.faforever.client.replay

import com.faforever.client.i18n.I18n
import com.faforever.client.task.CompletableTask
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LoadLocalReplaysTask @Inject
constructor(private val replayService: ReplayService, private val i18n: I18n) : CompletableTask<Collection<Replay>>(CompletableTask.Priority.HIGH) {

    @Throws(Exception::class)
    override fun call(): Collection<Replay> {
        updateTitle(i18n.get("replays.loadingLocalTask.title"))
        return replayService.localReplays
    }

}
