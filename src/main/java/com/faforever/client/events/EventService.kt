package com.faforever.client.events

import com.faforever.client.api.FafApiAccessor
import com.faforever.client.api.dto.PlayerEvent
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor

import java.util.function.Function.identity
import java.util.stream.Collectors.toMap


@Lazy
@Service
class EventService @Inject
constructor(private val fafApiAccessor: FafApiAccessor, private val threadPoolExecutor: ThreadPoolExecutor) {

    fun getPlayerEvents(playerId: Int): CompletableFuture<Map<String, PlayerEvent>> {
        return CompletableFuture.supplyAsync({
            fafApiAccessor.getPlayerEvents(playerId).stream()
                    .collect<Map<Any, PlayerEvent>, Any>(toMap<Any, Any, Any>({ playerEvent -> playerEvent.getEvent().getId() }, identity()))
        }, threadPoolExecutor)
    }

    companion object {

        val EVENT_CUSTOM_GAMES_PLAYED = "cfa449a6-655b-48d5-9a27-6044804fe35c"
        val EVENT_LADDER_1V1_GAMES_PLAYED = "4a929def-e347-45b4-b26d-4325a3115859"
        val EVENT_LOST_ACUS = "d6a699b7-99bc-4a7f-b128-15e1e289a7b3"
        val EVENT_BUILT_AIR_UNITS = "3ebb0c4d-5e92-4446-bf52-d17ba9c5cd3c"
        val EVENT_LOST_AIR_UNITS = "225e9b2e-ae09-4ae1-a198-eca8780b0fcd"
        val EVENT_BUILT_LAND_UNITS = "ea123d7f-bb2e-4a71-bd31-88859f0c3c00"
        val EVENT_LOST_LAND_UNITS = "a1a3fd33-abe2-4e56-800a-b72f4c925825"
        val EVENT_BUILT_NAVAL_UNITS = "b5265b42-1747-4ba1-936c-292202637ce6"
        val EVENT_LOST_NAVAL_UNITS = "3a7b3667-0f79-4ac7-be63-ba841fd5ef05"
        val EVENT_SECONDS_PLAYED = "cc791f00-343c-48d4-b5b3-8900b83209c0"
        val EVENT_BUILT_TECH_1_UNITS = "a8ee4f40-1e30-447b-bc2c-b03065219795"
        val EVENT_LOST_TECH_1_UNITS = "3dd3ed78-ce78-4006-81fd-10926738fbf3"
        val EVENT_BUILT_TECH_2_UNITS = "89d4f391-ed2d-4beb-a1ca-6b93db623c04"
        val EVENT_LOST_TECH_2_UNITS = "aebd750b-770b-4869-8e37-4d4cfdc480d0"
        val EVENT_BUILT_TECH_3_UNITS = "92617974-8c1f-494d-ab86-65c2a95d1486"
        val EVENT_LOST_TECH_3_UNITS = "7f15c2be-80b7-4573-8f41-135f84773e0f"
        val EVENT_BUILT_EXPERIMENTALS = "ed9fd79d-5ec7-4243-9ccf-f18c4f5baef1"
        val EVENT_LOST_EXPERIMENTALS = "701ca426-0943-4931-85af-6a08d36d9aaa"
        val EVENT_BUILT_ENGINEERS = "60bb1fc0-601b-45cd-bd26-83b1a1ac979b"
        val EVENT_LOST_ENGINEERS = "e8e99a68-de1b-4676-860d-056ad2207119"
        val EVENT_AEON_PLAYS = "96ccc66a-c5a0-4f48-acaa-888b00778b57"
        val EVENT_AEON_WINS = "a6b51c26-64e6-4e7a-bda7-ea1cfe771ebb"
        val EVENT_CYBRAN_PLAYS = "ad193982-e7ca-465c-80b0-5493f9739559"
        val EVENT_CYBRAN_WINS = "56b06197-1890-42d0-8b59-25e1add8dc9a"
        val EVENT_UEF_PLAYS = "1b900d26-90d2-43d0-a64e-ed90b74c3704"
        val EVENT_UEF_WINS = "7be6fdc5-7867-4467-98ce-f7244a66625a"
        val EVENT_SERAPHIM_PLAYS = "fefcb392-848f-4836-9683-300b283bc308"
        val EVENT_SERAPHIM_WINS = "15b6c19a-6084-4e82-ada9-6c30e282191f"
    }
}
