package com.faforever.client.chat

import javafx.scene.paint.Color

import java.util.Random

class ColorGeneratorUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        @JvmOverloads
        fun generateRandomColor(seed: Long = 0): Color {
            val goldenRatioConj = (1.0 + Math.sqrt(5.0)) / 2.0
            val saturation: Float
            var hue: Float
            saturation = randFloat(0.5f, 0.7f, seed)
            hue = Random(seed).nextFloat()
            hue += goldenRatioConj.toFloat()
            hue = hue % 1

            return Color.hsb((hue * 360).toDouble(), saturation.toDouble(), 0.9)
        }

        private fun randFloat(min: Float, max: Float, seed: Long): Float {
            val generator = Random(seed)
            return if (generator.nextDouble() < 0.5) {
                ((1 - generator.nextDouble()) * (max - min) + min).toFloat()
            } else (generator.nextDouble() * (max - min) + min).toFloat()

        }
    }
}//http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
