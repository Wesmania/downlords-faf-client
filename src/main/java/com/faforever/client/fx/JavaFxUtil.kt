package com.faforever.client.fx

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.IntegerProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.util.Duration
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import lombok.SneakyThrows

import java.awt.image.BufferedImage
import java.lang.reflect.Field
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays

import com.github.nocatch.NoCatch.noCatch
import com.sun.jna.platform.win32.WinUser.GWL_STYLE
import java.nio.file.Files.createDirectories
import javax.imageio.ImageIO.write

/**
 * Utility class to fix some annoying JavaFX shortcomings.
 */
class JavaFxUtil private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        val PATH_STRING_CONVERTER: StringConverter<Path> = object : StringConverter<Path>() {
            override fun toString(`object`: Path?): String? {
                return `object`?.toAbsolutePath()?.toString()
            }

            override fun fromString(string: String?): Path? {
                return if (string == null) {
                    null
                } else Paths.get(string)
            }
        }

        fun makeNumericTextField(textField: TextField, maxLength: Int) {
            JavaFxUtil.addListener<String>(textField.textProperty(), { observable, oldValue, newValue ->
                var value = newValue
                if (!value.matches("\\d*".toRegex())) {
                    value = newValue.replace("[^\\d]".toRegex(), "")
                }

                if (maxLength > 0 && value.length > maxLength) {
                    value = value.substring(0, maxLength)
                }

                textField.text = value
                if (textField.caretPosition > textField.length) {
                    textField.positionCaret(textField.length)
                }
            })
        }

        /**
         * Uses reflection to change to tooltip delay/duration to some sane values.
         *
         *
         * See [https://javafx-jira.kenai.com/browse/RT-19538](https://javafx-jira.kenai.com/browse/RT-19538)
         */
        fun fixTooltipDuration() {
            noCatch {
                val fieldBehavior = Tooltip::class.java.getDeclaredField("BEHAVIOR")
                fieldBehavior.isAccessible = true
                val objBehavior = fieldBehavior.get(null)

                val activationTimerField = objBehavior.javaClass.getDeclaredField("activationTimer")
                activationTimerField.isAccessible = true
                var objTimer = activationTimerField.get(objBehavior) as Timeline

                objTimer.keyFrames.setAll(KeyFrame(Duration(500.0)))

                val hideTimerField = objBehavior.javaClass.getDeclaredField("hideTimer")
                hideTimerField.isAccessible = true
                objTimer = hideTimerField.get(objBehavior) as Timeline

                objTimer.keyFrames.setAll(KeyFrame(Duration(100000.0)))
            }
        }

        /**
         * Centers a window FOR REAL. https://javafx-jira.kenai.com/browse/RT-40368
         */
        fun centerOnScreen(stage: Stage) {
            val width = stage.width
            val height = stage.height

            val screenBounds = Screen.getPrimary().visualBounds
            stage.x = (screenBounds.maxX - screenBounds.minX - width) / 2
            stage.y = (screenBounds.maxY - screenBounds.minY - height) / 2
        }

        fun assertApplicationThread() {
            if (!Platform.isFxApplicationThread()) {
                throw IllegalStateException("Must run in FX Application thread")
            }
        }

        fun assertBackgroundThread() {
            if (Platform.isFxApplicationThread()) {
                throw IllegalStateException("Must not run in FX Application thread")
            }
        }

        fun isVisibleRecursively(node: Node): Boolean {
            if (!node.isVisible) {
                return false
            }

            val parent = node.parent ?: return node.scene != null
            return isVisibleRecursively(parent)
        }

        fun toRgbCode(color: Color): String {
            return String.format("#%02X%02X%02X",
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt())
        }

        /**
         * Updates the specified list with any changes made to the specified map, but not vice versa.
         */
        fun <K, V> attachListToMap(list: ObservableList<V>, map: ObservableMap<K, V>) {
            addListener(map, { change ->
                synchronized(list) {
                    if (change.wasRemoved()) {
                        list.remove(change.getValueRemoved())
                    } else if (change.wasAdded()) {
                        list.add(change.getValueAdded())
                    }
                }
            } as MapChangeListener<K, V>)
        }

        fun persistImage(image: Image?, path: Path, format: String) {
            if (image == null) {
                return
            }
            if (image.isBackgroundLoading && image.progress < 1) {
                // Let's hope that loading doesn't finish before the listener is added
                JavaFxUtil.addListener(image.progressProperty(), object : ChangeListener<Number> {
                    override fun changed(observable: ObservableValue<out Number>, oldValue: Number, newValue: Number) {
                        if (newValue.toInt() >= 1) {
                            writeImage(image, path, format)
                            image.progressProperty().removeListener(this)
                        }
                    }
                })
            } else {
                writeImage(image, path, format)
            }
        }

        @SneakyThrows
        private fun writeImage(image: Image?, path: Path, format: String) {
            if (image == null) {
                return
            }
            if (path.parent != null) {
                createDirectories(path.parent)
            }
            val bufferedImage = SwingFXUtils.fromFXImage(image, null) ?: return
            write(bufferedImage, format, path.toFile())
        }

        fun setAnchors(node: Node, value: Double) {
            AnchorPane.setBottomAnchor(node, value)
            AnchorPane.setLeftAnchor(node, value)
            AnchorPane.setRightAnchor(node, value)
            AnchorPane.setTopAnchor(node, value)
        }

        fun fixScrollSpeed(scrollPane: ScrollPane) {
            val content = scrollPane.content
            content.setOnScroll { event ->
                val deltaY = event.deltaY * 3
                val height = scrollPane.content.boundsInLocal.height
                val vvalue = scrollPane.vvalue
                // deltaY/height to make the scrolling equally fast regardless of the actual height of the component
                scrollPane.vvalue = vvalue + -deltaY / height
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <T> addListener(observableValue: ObservableValue<T>, listener: ChangeListener<in T>) {
            synchronized(observableValue) {
                observableValue.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun addListener(observable: Observable, listener: InvalidationListener) {
            synchronized(observable) {
                observable.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <K, V> addListener(observable: ObservableMap<K, V>, listener: MapChangeListener<K, V>) {
            synchronized(observable) {
                observable.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <T> addListener(observable: ObservableList<T>, listener: ListChangeListener<T>) {
            synchronized(observable) {
                observable.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <K, V> addListener(mapProperty: MapProperty<K, V>, listener: MapChangeListener<in K, in V>) {
            synchronized(mapProperty) {
                mapProperty.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <T> addListener(set: ObservableSet<T>, listener: SetChangeListener<T>) {
            synchronized(set) {
                set.addListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, removing listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun <T> removeListener(observableValue: ObservableValue<T>, listener: ChangeListener<in T>) {
            synchronized(observableValue) {
                observableValue.removeListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, removing listeners must be synchronized on the property - which
         * is what this method does.
         */
        fun removeListener(observable: Observable, listener: InvalidationListener) {
            synchronized(observable) {
                observable.removeListener(listener)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, binding a property must be synchronized on the property - which
         * is what this method does.
         */
        fun <T> bind(property: Property<T>, observable: ObservableValue<out T>) {
            synchronized(property) {
                property.bind(observable)
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, binding properties must be synchronized on the properties -
         * which is what this method does. Since synchronization happens on both property in order `property1,
         * property2`, this is prone to deadlocks. To avoid this, pass the property with the lower visibility (e.g. method- or
         * controller-only) as first and the property with higher visibility (e.g. a property from a shared object or service)
         * as second parameter.
         */
        fun bindBidirectional(stringProperty: StringProperty, integerProperty: IntegerProperty, numberStringConverter: NumberStringConverter) {
            synchronized(stringProperty) {
                synchronized(integerProperty) {
                    stringProperty.bindBidirectional(integerProperty, numberStringConverter)
                }
            }
        }

        /**
         * Since the JavaFX properties API is not thread safe, binding properties must be synchronized on the properties -
         * which is what this method does. Since synchronization happens on both property in order `property1,
         * property2`, this is prone to deadlocks. To avoid this, pass the property with the lower visibility (e.g. method- or
         * controller-only) as first and the property with higher visibility (e.g. a property from a shared object or service)
         * as second parameter.
         */
        fun <T> bindBidirectional(property1: Property<T>, property2: Property<T>) {
            synchronized(property1) {
                synchronized(property2) {
                    property1.bindBidirectional(property2)
                }
            }
        }

        val nativeWindow: Pointer
            get() = User32.INSTANCE.GetActiveWindow().pointer

        fun runLater(runnable: Runnable) {
            if (Platform.isFxApplicationThread()) {
                runnable.run()
            } else {
                Platform.runLater(runnable)
            }
        }

        fun bindManagedToVisible(vararg nodes: Node) {
            Arrays.stream(nodes).forEach { node -> node.managedProperty().bind(node.visibleProperty()) }
        }


        /**
         * Uniconifies stages when clicking on the icon in the task bar. Source: http://stackoverflow.com/questions/26972683/javafx-minimizing-undecorated-stage
         * Bug report: https://bugs.openjdk.java.net/browse/JDK-8089296
         */
        fun fixJDK8089296() {
            if (!org.bridj.Platform.isWindows()) {
                return
            }
            val lpVoid = nativeWindow
            val hwnd = WinDef.HWND(lpVoid)
            val user32 = User32.INSTANCE
            val newStyle = user32.GetWindowLong(hwnd, GWL_STYLE) or 0x00020000 //WS_MINIMIZEBOX
            user32.SetWindowLong(hwnd, GWL_STYLE, newStyle)
        }
    }
}
