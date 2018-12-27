package com.faforever.client.api

import com.github.jasminb.jsonapi.JSONAPIDocument
import com.github.jasminb.jsonapi.ReflectionUtils
import com.github.jasminb.jsonapi.ResourceConverter
import lombok.SneakyThrows
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.InputStream
import java.text.MessageFormat

@Component
class JsonApiMessageConverter @Inject
constructor(private val resourceConverter: ResourceConverter) : AbstractHttpMessageConverter<Object>(MediaType.parseMediaType("application/vnd.api+json")) {

    @Override
    protected fun supports(clazz: Class<*>): Boolean {
        return Collection::class.java!!.isAssignableFrom(clazz) || ReflectionUtils.getTypeName(clazz) != null
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected fun readInternal(clazz: Class<*>, inputMessage: HttpInputMessage): Object {
        inputMessage.getBody().use({ inputStream ->
            val document: JSONAPIDocument<*>
            if (Iterable::class.java!!.isAssignableFrom(clazz)) {
                document = resourceConverter.readDocumentCollection(inputStream, Object::class.java)
            } else {
                document = resourceConverter.readDocument(inputMessage.getBody(), Object::class.java)
            }

            return document.get()
        })
    }

    @Override
    @SneakyThrows
    protected fun writeInternal(o: Object, outputMessage: HttpOutputMessage) {
        val serializedObject: ByteArray
        if (o is Iterable) {
            serializedObject = resourceConverter.writeDocumentCollection(JSONAPIDocument<Iterable<*>>(o as Iterable<*>))
        } else {
            serializedObject = resourceConverter.writeDocument(JSONAPIDocument(o))
        }
        logger.trace(MessageFormat.format("Serialized ''{0}'' as ''{1}''", o, String(serializedObject)))
        outputMessage.getBody().write(serializedObject)
    }
}
