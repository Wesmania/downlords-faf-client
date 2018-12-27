package com.faforever.client.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.io.IOException

@Configuration
class JacksonConfig {

    @Bean
    fun customDeserializers(): Module {
        val fafModule = SimpleModule()
        fafModule.addDeserializer(ComparableVersion::class.java, ComparableVersionDeserializer())
        return fafModule
    }

    private class ComparableVersionDeserializer : JsonDeserializer<ComparableVersion>() {

        @Throws(IOException::class)
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ComparableVersion {
            return ComparableVersion(p.valueAsString)
        }
    }
}
