package com.faforever.client.util

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage

import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class IdenticonUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        private val PIXEL_COUNT = 8
        private val IMAGE_SIZE = 128

        fun createIdenticon(`object`: Any): Image {
            try {
                val hash = MessageDigest.getInstance("MD5").digest(`object`.toString().toByteArray())

                val bufferedImage = BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR)
                val pixelSize = IMAGE_SIZE / PIXEL_COUNT

                val graphics = bufferedImage.graphics
                graphics.color = Color(hash[0] and 255, hash[1] and 255, hash[2] and 255)

                val mirrorPixel = Math.ceil((PIXEL_COUNT / 2f).toDouble()).toInt()
                for (x in 0 until PIXEL_COUNT) {
                    val pixelDeterminingIndex = if (x < mirrorPixel) x else PIXEL_COUNT - 1 - x

                    for (y in 0 until PIXEL_COUNT) {
                        if (hash[pixelDeterminingIndex] shr y and 1 == 1) {
                            graphics.fillRect(x * pixelSize, y * pixelSize, pixelSize, pixelSize)
                        }
                    }
                }

                return SwingFXUtils.toFXImage(bufferedImage, WritableImage(bufferedImage.width, bufferedImage.height))
            } catch (ex: NoSuchAlgorithmException) {
                throw RuntimeException(ex)
            }

        }
    }
}
