package com.faforever.client.api.dto


import com.github.jasminb.jsonapi.models.errors.Error
import java.util.stream.Collectors

class ApiException(private val errors: List<Error>) : RuntimeException() {

    // TODO localize
    val localizedMessage: String
        @Override
        get() {
            return errors.stream()
                    .map({ Error.getDetail() })
                    .collect(Collectors.joining("\n"))
        }
}
