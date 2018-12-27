package com.faforever.client.remote.domain

import com.google.gson.annotations.SerializedName

class AvatarMessage : FafServerMessage() {

    @SerializedName("avatarlist")
    val avatarList: List<Avatar>? = null
}
