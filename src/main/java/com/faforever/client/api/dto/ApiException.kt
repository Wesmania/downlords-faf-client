package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.models.errors.Error
import java.util.stream.Collectors

class ApiException(private val errors: List<Error>) : RuntimeException() {

    // TODO localize
    override fun getLocalizedMessage(): String {
            return errors.stream()
                    .map { it.detail }
                    .collect(Collectors.joining("\n"))
        }
}
