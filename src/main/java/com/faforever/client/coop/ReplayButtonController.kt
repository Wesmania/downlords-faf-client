package com.faforever.client.coop

import com.faforever.client.fx.Controller
import javafx.scene.control.Button
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.function.Consumer


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReplayButtonController : Controller<Button> {
    override var root: Button? = null
    var replayId: String? = null
    private var onReplayButtonClicked: Consumer<ReplayButtonController>? = null

    internal fun setOnClickedAction(onReplayButtonClicked: Consumer<ReplayButtonController>) {
        this.onReplayButtonClicked = onReplayButtonClicked
    }

    fun onClicked() {
        onReplayButtonClicked!!.accept(this)
    }
}
