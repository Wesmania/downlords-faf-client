package com.faforever.client.preferences

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import lombok.extern.slf4j.Slf4j

import java.nio.charset.StandardCharsets
import java.util.Base64

@Slf4j
class LoginPrefs {

    private val username: StringProperty
    private val password: StringProperty
    private val autoLogin: BooleanProperty

    init {
        username = SimpleStringProperty()
        password = SimpleStringProperty()
        autoLogin = SimpleBooleanProperty()

    }

    fun getUsername(): String {
        return username.get()
    }

    fun setUsername(username: String): LoginPrefs {
        this.username.set(username)
        return this
    }

    fun usernameProperty(): StringProperty {
        return username
    }

    fun getPassword(): String? {
        // FIXME remove poor man's security once refresh tokens are implemented
        try {
            return deObfuscate(password.get())
        } catch (e: Exception) {
            log.warn("Could not deobfuscate password", e)
            setAutoLogin(false)
            return null
        }

    }

    fun setPassword(password: String): LoginPrefs {
        // FIXME remove poor man's security once refresh tokens are implemented
        this.password.set(obfuscate(password))
        return this
    }

    fun passwordProperty(): StringProperty {
        return password
    }

    fun getAutoLogin(): Boolean {
        return autoLogin.get()
    }

    fun setAutoLogin(autoLogin: Boolean): LoginPrefs {
        this.autoLogin.set(autoLogin)
        return this
    }

    fun autoLoginProperty(): BooleanProperty {
        return autoLogin
    }

    private fun obfuscate(string: String?): String? {
        if (string == null) {
            return null
        }
        val result = CharArray(string.length)
        for (i in 0 until string.length) {
            result[i] = string[i] + KEY[i % KEY.length]
        }

        return Base64.getEncoder().encodeToString(String(result).toByteArray(StandardCharsets.UTF_8))
    }

    private fun deObfuscate(string: String?): String? {
        if (string == null) {
            return null
        }
        val innerString = String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8)
        val result = CharArray(innerString.length)
        for (i in 0 until innerString.length) {
            result[i] = (innerString[i] - KEY[i % KEY.length]).toChar()
        }

        return String(result)
    }

    companion object {

        private val KEY = System.getProperty("user.name")
    }
}
