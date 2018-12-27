package com.faforever.client.config

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jasminb.jsonapi.ResourceConverter
import com.github.jasminb.jsonapi.annotations.Type
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.stream.Collectors

import com.github.nocatch.NoCatch.noCatch
import java.lang.Class.forName

@Configuration
class JsonApiConfig {

    @Bean
    fun resourceConverter(objectMapper: ObjectMapper): ResourceConverter {
        objectMapper.setSerializationInclusion(Include.NON_NULL)
        return ResourceConverter(objectMapper, *findJsonApiTypes("com.faforever.client.api.dto"))
    }

    private fun findJsonApiTypes(scanPackage: String): Array<Class<*>> {
        val provider = ClassPathScanningCandidateComponentProvider(false)
        provider.addIncludeFilter(AnnotationTypeFilter(Type::class.java))
        val classes = provider.findCandidateComponents(scanPackage).stream()
                .map<Class<*>> { beanDefinition -> noCatch<Class> { forName(beanDefinition.beanClassName) as Class<*> } }
                .collect<List<Class<*>>, Any>(Collectors.toList<Class>())
        return classes.toTypedArray()
    }
}
