package com.faforever.client.chat

import javafx.scene.image.Image
import lombok.SneakyThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.Arrays
import java.util.Optional

import com.faforever.client.config.CacheNames.COUNTRY_FLAGS

@Lazy
@Service
class CountryFlagService {

    @Cacheable(COUNTRY_FLAGS)
    fun loadCountryFlag(country: String?): Optional<Image> {
        return if (country == null) {
            Optional.empty()
        } else getCountryFlagUrl(country)
                .map { url -> Image(url.toString(), true) }

    }

    @SneakyThrows
    fun getCountryFlagUrl(country: String?): Optional<URL> {
        if (country == null) {
            return Optional.empty()
        }
        val imageName: String
        if (NON_COUNTRY_CODES.contains(country)) {
            imageName = "earth"
        } else {
            imageName = country.toLowerCase()
        }

        val path = "/images/flags/$imageName.png"
        val classPathResource = ClassPathResource(path)
        return if (!classPathResource.exists()) {
            Optional.empty()
        } else Optional.of(classPathResource.url)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val NON_COUNTRY_CODES = Arrays.asList("A1", "A2", "")
    }
}
