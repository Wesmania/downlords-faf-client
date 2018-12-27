package com.faforever.client.remote.domain

import com.google.gson.annotations.SerializedName

class RestoreGameSessionMessage(@field:SerializedName("game_id")
                                private val gameId: Int) : ClientMessage(ClientMessageType.RESTORE_GAME_SESSION)
