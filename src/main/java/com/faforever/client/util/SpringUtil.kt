package com.faforever.client.util

import com.google.common.collect.ImmutableMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestOperations
import java.util.*
import java.util.stream.Collectors

inline fun <reified T: Any> typeRef(): ParameterizedTypeReference<T> = object: ParameterizedTypeReference<T>(){}

fun<K, V> mapToMultiMap(sm: Map<K, V>): MultiValueMap<K, V> {
    val multiValues: kotlin.collections.Map<K, List<V>> = sm.entries.stream()
            .collect(Collectors.toMap({ it.key }, { listOf(it.value) }))
    return CollectionUtils.toMultiValueMap(multiValues)
}

/* For some reason Spring doesn't have this variant (yet?) */
fun<T> RestOperations.getForObject(url: String, responseType: ParameterizedTypeReference<T>) : T? {
    var response = this.exchange(url, HttpMethod.GET, null, responseType)
    return response.body
}

fun<T> RestOperations.getForObject(url: String, map: Map<String, String>, responseType: ParameterizedTypeReference<T>) : T? {
    var response = this.exchange(url, HttpMethod.GET, null, responseType, map)
    return response.body
}
