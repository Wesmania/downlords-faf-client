package com.faforever.client.config

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

import javax.annotation.PreDestroy
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@EnableAsync
@EnableScheduling
@Configuration
class AsyncConfig : AsyncConfigurer, SchedulingConfigurer {

    override fun getAsyncExecutor(): Executor? {
        return taskExecutor()
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? {
        return SimpleAsyncUncaughtExceptionHandler()
    }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler())
    }

    @Bean
    fun taskExecutor(): ExecutorService {
        return Executors.newCachedThreadPool()
    }

    @Bean
    fun taskScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler()
    }

    @PreDestroy
    fun preDestroy() {
        taskExecutor().shutdownNow()
    }
}
