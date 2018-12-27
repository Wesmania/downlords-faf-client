package com.faforever.client.uploader.imgur

import com.faforever.client.task.TaskService
import com.faforever.client.uploader.ImageUploadService
import javafx.scene.image.Image
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.concurrent.CompletableFuture


@Lazy
@Service
class ImgurImageUploadService @Inject
constructor(private val taskService: TaskService, private val applicationContext: ApplicationContext) : ImageUploadService {

    override fun uploadImageInBackground(image: Image): CompletableFuture<String> {
        val imgurUploadTask = applicationContext.getBean(ImgurUploadTask::class.java)
        imgurUploadTask.setImage(image)
        return taskService.submitTask(imgurUploadTask).future
    }
}
