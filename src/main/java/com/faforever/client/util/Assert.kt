package com.faforever.client.util

class Assert private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        fun checkNullArgument(`object`: Any?, message: String) {
            if (`object` == null) {
                throw IllegalArgumentException(message)
            }
        }

        fun checkNullIllegalState(`object`: Any?, message: String) {
            if (`object` == null) {
                throw IllegalStateException(message)
            }
        }

        fun checkNotNullIllegalState(`object`: Any?, message: String) {
            if (`object` != null) {
                throw IllegalStateException(message)
            }
        }
    }
}
