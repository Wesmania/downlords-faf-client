package com.faforever.client.api


import com.faforever.client.api.dto.ApiException
import com.github.jasminb.jsonapi.exceptions.ResourceParseException
import com.github.jasminb.jsonapi.models.errors.Errors
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler

import java.io.IOException

@Component
class JsonApiErrorHandler(private val jsonApiMessageConverter: JsonApiMessageConverter) : DefaultResponseErrorHandler() {

    @Override
    @Throws(IOException::class)
    fun handleError(response: ClientHttpResponse) {
        if (response.getStatusCode() === HttpStatus.UNPROCESSABLE_ENTITY) {
            try {
                jsonApiMessageConverter.readInternal(Errors::class.java, response)
            } catch (e: ResourceParseException) {
                throw ApiException(e.getErrors().getErrors())
            }

        }
        super.handleError(response)
    }
}
