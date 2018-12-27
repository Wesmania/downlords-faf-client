package com.faforever.client.ui.tray

import com.faforever.client.i18n.I18n
import com.faforever.client.ui.StageHolder
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.VPos
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.TextAlignment
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.stream.Collectors
import java.util.stream.IntStream

import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON

@Component
class TrayIconManager @Inject
constructor(private val i18n: I18n, private val eventBus: EventBus) {
    private var badgeCount: Int = 0

    @PostConstruct
    fun postConstruct() {
        eventBus.register(this)
    }

    /**
     * Generates and returns a tray icon. If `badgeCount` is greater than 0, a badge (circle) with the badge count
     * generated on top of the icon.
     */
    @Subscribe
    fun onSetApplicationBadgeEvent(event: UpdateApplicationBadgeEvent) {
        Platform.runLater {
            if (event.delta.isPresent) {
                badgeCount += event.delta.get()
            } else if (event.newValue.isPresent) {
                badgeCount = event.newValue.get()
            } else {
                throw IllegalStateException("No delta nor new value is available")
            }

            val icons: List<Image>
            if (badgeCount < 1) {
                icons = IntStream.range(4, 9)
                        .mapToObj { power -> generateTrayIcon(Math.pow(2.0, power.toDouble()).toInt()) }
                        .collect<List<Image>, Any>(Collectors.toList())
            } else {
                icons = IntStream.range(4, 9)
                        .mapToObj { power -> generateTrayIcon(Math.pow(2.0, power.toDouble()).toInt()) }
                        .map { image -> addBadge(image, badgeCount) }
                        .collect<List<Image>, Any>(Collectors.toList())
            }
            StageHolder.stage.icons.setAll(icons)
        }
    }

    private fun addBadge(icon: Image, badgeCount: Int): Image {
        val badgeIconSize = (icon.width * 0.6f).toInt()

        val appIcon = SwingFXUtils.fromFXImage(icon, null)

        val appIconGraphics = appIcon.createGraphics()
        appIconGraphics.font = Font(Font.SANS_SERIF, Font.BOLD, (badgeIconSize * .8).toInt())
        appIconGraphics.setRenderingHints(RenderingHints(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON))
        appIconGraphics.color = java.awt.Color(244, 67, 54)

        val badgeX = appIcon.width - badgeIconSize
        val badgeY = appIcon.height - badgeIconSize
        appIconGraphics.fillOval(badgeX, badgeY, badgeIconSize, badgeIconSize)

        val numberText = i18n.number(badgeCount)

        val numberX = appIcon.width - badgeIconSize / 2 - appIconGraphics.fontMetrics.stringWidth(numberText) / 2
        val numberY = appIcon.height - badgeIconSize / 2 + (appIconGraphics.fontMetrics.ascent - appIconGraphics.fontMetrics.descent) / 2

        appIconGraphics.color = java.awt.Color.WHITE
        appIconGraphics.drawString(numberText, numberX, numberY)
        return SwingFXUtils.toFXImage(appIcon, WritableImage(appIcon.width, appIcon.height))
    }

    private fun generateTrayIcon(dimension: Int): Image {
        val canvas = Canvas(dimension.toDouble(), dimension.toDouble())

        val writableImage = WritableImage(dimension, dimension)

        val graphicsContext2D = canvas.graphicsContext2D
        graphicsContext2D.textAlign = TextAlignment.CENTER
        graphicsContext2D.textBaseline = VPos.CENTER
        graphicsContext2D.fontSmoothingType = FontSmoothingType.LCD
        graphicsContext2D.font = javafx.scene.text.Font.loadFont(TrayIconManager::class.java.getResourceAsStream("/font/dfc-icons.ttf"), dimension.toDouble())
        graphicsContext2D.fill = Color.BLACK
        graphicsContext2D.fillOval(0.0, 0.0, dimension.toDouble(), dimension.toDouble())
        graphicsContext2D.fill = Color.WHITE
        graphicsContext2D.fillText("\uE901", (dimension / 2).toDouble(), (dimension / 2).toDouble())

        val snapshotParameters = SnapshotParameters()
        snapshotParameters.fill = javafx.scene.paint.Color.TRANSPARENT
        return fixImage(canvas.snapshot(snapshotParameters, writableImage))
    }

    /**
     * See [http://stackoverflow.com/questions/41029931/snapshot-image-cant-be-used-as-stage-icon](http://stackoverflow.com/questions/41029931/snapshot-image-cant-be-used-as-stage-icon)
     */
    private fun fixImage(image: Image): Image {
        return SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(image, null), null)
    }
}
