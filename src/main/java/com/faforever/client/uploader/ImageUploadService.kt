package com.faforever.client.uploader

import javafx.scene.image.Image

import java.util.concurrent.CompletableFuture

interface ImageUploadService {

    fun uploadImageInBackground(image: Image): CompletableFuture<String>
}
