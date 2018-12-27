package com.faforever.client.user

import com.faforever.client.api.FafApiAccessor
import com.faforever.client.i18n.I18n
import com.faforever.client.task.CompletableTask
import com.faforever.client.util.Validator
import com.google.common.hash.Hashing
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject

import java.nio.charset.StandardCharsets.UTF_8

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ChangePasswordTask @Inject
constructor(private val fafApiAccessor: FafApiAccessor, private val i18n: I18n) : CompletableTask<Void>(CompletableTask.Priority.HIGH) {

    private var currentPassword: String? = null
    private var newPassword: String? = null
    private var username: String? = null

    @PostConstruct
    internal fun postConstruct() {
        updateTitle(i18n.get("settings.account.changePassword.changing"))
    }

    internal fun setCurrentPassword(currentPassword: String) {
        this.currentPassword = currentPassword
    }

    internal fun setNewPassword(newPassword: String) {
        this.newPassword = newPassword
    }

    internal fun setUsername(username: String) {
        this.username = username
    }

    @Throws(Exception::class)
    override fun call(): Void? {
        Validator.notNull(currentPassword, "currentPassword must not be null")
        Validator.notNull(newPassword, "newPassword must not be null")

        val currentPasswordHash = Hashing.sha256().hashString(currentPassword!!, UTF_8).toString()
        val newPasswordHash = Hashing.sha256().hashString(newPassword!!, UTF_8).toString()

        fafApiAccessor.changePassword(username!!, currentPasswordHash, newPasswordHash)

        return null
    }
}
