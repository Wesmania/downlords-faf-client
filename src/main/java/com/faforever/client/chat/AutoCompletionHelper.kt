package com.faforever.client.chat

import com.faforever.client.player.PlayerService
import com.faforever.client.util.Assert
import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.ArrayList
import java.util.stream.Collectors

import java.util.Locale.US

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class AutoCompletionHelper @Inject
constructor(private val playerService: PlayerService) {

    private var possibleAutoCompletions: MutableList<String>? = null
    private var nextAutoCompleteIndex: Int = 0
    private var autoCompletePartialName: String? = null

    private var boundTextField: TextInputControl? = null
    private val keyEventHandler: EventHandler<KeyEvent>

    val isBound: Boolean
        get() = boundTextField != null

    init {

        keyEventHandler = { keyEvent ->
            if (!keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.TAB) {
                keyEvent.consume()
                autoComplete()
            }
        }
    }

    private fun autoComplete() {
        if (boundTextField!!.text.isEmpty()) {
            return
        }

        if (possibleAutoCompletions == null) {
            initializeAutoCompletion()

            if (possibleAutoCompletions!!.isEmpty()) {
                // There are no auto completion matches
                resetAutoCompletion()
                return
            }

            // It's the first auto complete event at this location, just replace the text with the first user name
            boundTextField!!.selectPreviousWord()
            boundTextField!!.replaceSelection(possibleAutoCompletions!![nextAutoCompleteIndex++])
            return
        }

        // At this point, it's a subsequent auto complete event
        val wordBeforeCaret = getWordBeforeCaret(boundTextField!!)

        /*
     * We have to check if the previous word is the one we auto completed. If not we reset and start all over again
     * as the user started auto completion on another word.
     */
        if (wordBeforeCaret != possibleAutoCompletions!![nextAutoCompleteIndex - 1]) {
            resetAutoCompletion()
            autoComplete()
            return
        }

        if (possibleAutoCompletions!!.size == 1) {
            // No need to cycle since there was only one match
            return
        }

        if (possibleAutoCompletions!!.size <= nextAutoCompleteIndex) {
            // Start over again in order to cycle
            nextAutoCompleteIndex = 0
        }

        boundTextField!!.selectPreviousWord()
        boundTextField!!.replaceSelection(possibleAutoCompletions!![nextAutoCompleteIndex++])
    }

    private fun initializeAutoCompletion() {
        possibleAutoCompletions = ArrayList()

        autoCompletePartialName = getWordBeforeCaret(boundTextField!!)
        if (autoCompletePartialName!!.isEmpty()) {
            return
        }

        nextAutoCompleteIndex = 0

        possibleAutoCompletions!!.addAll(
                playerService.playerNames.stream()
                        .filter { playerName -> playerName.toLowerCase(US).startsWith(autoCompletePartialName!!.toLowerCase()) }
                        .sorted()
                        .collect<List<String>, Any>(Collectors.toList())
        )
    }

    fun bindTo(messageTextField: TextInputControl) {
        Assert.checkNotNullIllegalState(boundTextField, "AutoCompletionHelper is already bound to a TextInputControl")
        boundTextField = messageTextField
        boundTextField!!.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler)
    }

    fun unbind() {
        if (boundTextField != null) {
            boundTextField!!.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler)
            boundTextField = null
        }
    }

    private fun getWordBeforeCaret(messageTextField: TextInputControl): String {
        messageTextField.selectPreviousWord()
        val selectedText = messageTextField.selectedText
        messageTextField.positionCaret(messageTextField.anchor)
        return selectedText
    }

    private fun resetAutoCompletion() {
        possibleAutoCompletions = null
        nextAutoCompleteIndex = -1
        autoCompletePartialName = null
    }
}
