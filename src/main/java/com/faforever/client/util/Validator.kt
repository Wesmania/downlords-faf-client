package com.faforever.client.util

import java.util.regex.Pattern

object Validator {

    private val INT_PATTERN = Pattern.compile("\\d+")

    /**
     * Throws a NullPointerException with the specified message when `object` is null.
     *
     * @param object the object to check for null
     * @param message the exception message
     */
    fun notNull(`object`: Any?, message: String) {
        if (`object` == null) {
            throw NullPointerException(message)
        }
    }

    fun isInt(string: String): Boolean {
        return INT_PATTERN.matcher(string).matches()
    }

}// Utility class
