package com.faforever.client.map

class MapLoadException : RuntimeException {

    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String) : super(message) {}
}
