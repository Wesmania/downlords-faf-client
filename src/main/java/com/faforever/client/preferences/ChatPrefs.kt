package com.faforever.client.preferences

import com.faforever.client.chat.ChatColorMode
import com.faforever.client.chat.ChatFormat
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.paint.Color

import com.faforever.client.chat.ChatColorMode.CUSTOM

class ChatPrefs {

    private val zoom: DoubleProperty
    private val learnedAutoComplete: BooleanProperty
    private val previewImageUrls: BooleanProperty
    private val maxMessages: IntegerProperty
    private val chatColorMode: ObjectProperty<ChatColorMode>
    private val channelTabScrollPaneWidth: IntegerProperty
    private val userToColor: MapProperty<String, Color>
    private val hideFoeMessages: BooleanProperty
    private val timeFormat: ObjectProperty<TimeInfo>
    private val chatFormat: ObjectProperty<ChatFormat>

    /**
     * Time in minutes a player has to be inactive to be considered idle.
     */
    private val idleThreshold: IntegerProperty

    init {
        timeFormat = SimpleObjectProperty(TimeInfo.AUTO)
        maxMessages = SimpleIntegerProperty(500)
        zoom = SimpleDoubleProperty(1.0)
        learnedAutoComplete = SimpleBooleanProperty(false)
        previewImageUrls = SimpleBooleanProperty(true)
        hideFoeMessages = SimpleBooleanProperty(true)
        channelTabScrollPaneWidth = SimpleIntegerProperty(250)
        userToColor = SimpleMapProperty(FXCollections.observableHashMap())
        chatColorMode = SimpleObjectProperty(CUSTOM)
        idleThreshold = SimpleIntegerProperty(10)
        chatFormat = SimpleObjectProperty(ChatFormat.COMPACT)
    }

    fun getChatColorMode(): ChatColorMode {
        return chatColorMode.get()
    }

    fun setChatColorMode(chatColorMode: ChatColorMode) {
        this.chatColorMode.set(chatColorMode)
    }

    fun getTimeFormat(): TimeInfo {
        return timeFormat.get()
    }

    fun setTimeFormat(time: TimeInfo) {
        this.timeFormat.set(time)
    }

    fun getChatFormat(): ChatFormat {
        return this.chatFormat.get()
    }

    fun setChatFormat(chatFormat: ChatFormat) {
        this.chatFormat.value = chatFormat
    }

    fun chatFormatProperty(): ObjectProperty<ChatFormat> {
        return chatFormat
    }

    fun chatColorModeProperty(): ObjectProperty<ChatColorMode> {
        return chatColorMode
    }

    fun getUserToColor(): ObservableMap<String, Color> {
        return userToColor.get()
    }

    fun setUserToColor(userToColor: ObservableMap<String, Color>) {
        this.userToColor.set(userToColor)
    }

    fun userToColorProperty(): MapProperty<String, Color> {
        return userToColor
    }

    fun getPreviewImageUrls(): Boolean {
        return previewImageUrls.get()
    }

    fun setPreviewImageUrls(previewImageUrls: Boolean) {
        this.previewImageUrls.set(previewImageUrls)
    }

    fun previewImageUrlsProperty(): BooleanProperty {
        return previewImageUrls
    }

    fun getZoom(): Double? {
        return zoom.value
    }

    fun setZoom(zoom: Double) {
        this.zoom.set(zoom)
    }

    fun zoomProperty(): DoubleProperty {
        return zoom
    }

    fun getLearnedAutoComplete(): Boolean {
        return learnedAutoComplete.get()
    }

    fun setLearnedAutoComplete(learnedAutoComplete: Boolean) {
        this.learnedAutoComplete.set(learnedAutoComplete)
    }

    fun learnedAutoCompleteProperty(): BooleanProperty {
        return learnedAutoComplete
    }

    fun getMaxMessages(): Int {
        return maxMessages.get()
    }

    fun setMaxMessages(maxMessages: Int) {
        this.maxMessages.set(maxMessages)
    }

    fun maxMessagesProperty(): IntegerProperty {
        return maxMessages
    }

    fun getChannelTabScrollPaneWidth(): Int {
        return channelTabScrollPaneWidth.get()
    }

    fun setChannelTabScrollPaneWidth(channelTabScrollPaneWidth: Int) {
        this.channelTabScrollPaneWidth.set(channelTabScrollPaneWidth)
    }

    fun channelTabScrollPaneWidthProperty(): IntegerProperty {
        return channelTabScrollPaneWidth
    }


    fun getHideFoeMessages(): Boolean {
        return hideFoeMessages.get()
    }

    fun setHideFoeMessages(hideFoeMessages: Boolean) {
        this.hideFoeMessages.set(hideFoeMessages)
    }

    fun hideFoeMessagesProperty(): BooleanProperty {
        return hideFoeMessages
    }

    fun getIdleThreshold(): Int {
        return idleThreshold.get()
    }

    fun setIdleThreshold(idleThreshold: Int) {
        this.idleThreshold.set(idleThreshold)
    }

    fun idleThresholdProperty(): IntegerProperty {
        return idleThreshold
    }
}
