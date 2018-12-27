package com.faforever.client.chat

import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class PircBotXFactoryImpl : PircBotXFactory {

    override fun createPircBotX(configuration: Configuration): PircBotX {
        return PircBotX(configuration)
    }
}
