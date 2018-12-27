package com.faforever.client.reporting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import java.lang.invoke.MethodHandles


@Lazy
@Service
class ReportingService {

    fun reportError(e: Throwable) {
        logger.warn("Reporting has not yet been implemented")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
