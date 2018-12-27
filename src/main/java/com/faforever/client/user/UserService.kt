package com.faforever.client.user

import com.faforever.client.login.LoginFailedException
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.remote.domain.NoticeMessage
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import com.faforever.client.user.event.LogOutRequestEvent
import com.faforever.client.user.event.LoggedOutEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.util.concurrent.CompletableFuture

@Lazy
@Service
class UserService @Inject
constructor(private val fafService: FafService, private val preferencesService: PreferencesService, private val eventBus: EventBus, private val applicationContext: ApplicationContext, private val taskService: TaskService) {
    private val username: StringProperty

    var password: String? = null
        private set
    var userId: Int? = null
        private set
    private var loginFuture: CompletableFuture<Void>? = null

    init {
        username = SimpleStringProperty()
    }


    fun login(username: String, password: String, autoLogin: Boolean): CompletableFuture<Void>? {
        this.password = password

        preferencesService.preferences!!.login
                .setUsername(username)
                .setPassword(password).autoLogin = autoLogin
        preferencesService.storeInBackground()

        loginFuture = fafService.connectAndLogIn(username, password)
                .thenAccept { loginInfo ->
                    userId = loginInfo.id

                    // Because of different case (upper/lower)
                    val login = loginInfo.login
                    this@UserService.username.set(login)

                    preferencesService.preferences!!.login.username = login
                    preferencesService.storeInBackground()

                    eventBus.post(LoginSuccessEvent(login, password, userId!!))
                }
                .whenComplete { aVoid, throwable ->
                    if (throwable != null) {
                        logger.warn("Error during login", throwable)
                        fafService.disconnect()
                    }
                    loginFuture = null
                }
        return loginFuture
    }


    fun getUsername(): String {
        return username.get()
    }


    fun cancelLogin() {
        if (loginFuture != null) {
            loginFuture!!.toCompletableFuture().cancel(true)
            loginFuture = null
            fafService.disconnect()
        }
    }

    private fun onLoginError(noticeMessage: NoticeMessage) {
        if (loginFuture != null) {
            loginFuture!!.toCompletableFuture().completeExceptionally(LoginFailedException(noticeMessage.text))
            loginFuture = null
            fafService.disconnect()
        }
    }

    fun logOut() {
        logger.info("Logging out")
        fafService.disconnect()
        eventBus.post(LoggedOutEvent())
        preferencesService.preferences!!.login.autoLogin = false
    }


    fun changePassword(currentPassword: String, newPassword: String): CompletableTask<Void> {
        val changePasswordTask = applicationContext.getBean(ChangePasswordTask::class.java)
        changePasswordTask.setUsername(username.get())
        changePasswordTask.setCurrentPassword(currentPassword)
        changePasswordTask.setNewPassword(newPassword)

        return taskService.submitTask(changePasswordTask)
    }

    @PostConstruct
    internal fun postConstruct() {
        fafService.addOnMessageListener(LoginMessage::class.java) { loginInfo -> userId = loginInfo.id }
        fafService.addOnMessageListener(NoticeMessage::class.java, Consumer<NoticeMessage> { this.onLoginError(it) })
        eventBus.register(this)
    }

    @Subscribe
    fun onLogoutRequestEvent(event: LogOutRequestEvent) {
        logOut()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
