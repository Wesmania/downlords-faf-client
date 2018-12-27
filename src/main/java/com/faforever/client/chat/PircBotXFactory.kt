package com.faforever.client.chat

import org.pircbotx.Configuration
import org.pircbotx.PircBotX

interface PircBotXFactory {

    fun createPircBotX(configuration: Configuration): PircBotX
}
