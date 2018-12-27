package com.faforever.client.audio

import javafx.scene.media.AudioClip
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class AudioClipPlayer {

    fun playSound(audioClip: AudioClip) {
        audioClip.play()
    }
}
