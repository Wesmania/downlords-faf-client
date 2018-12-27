package com.faforever.client.rankedmatch

import com.faforever.client.remote.domain.FafServerMessage
import com.faforever.client.remote.domain.FafServerMessageType
import com.faforever.client.remote.domain.RatingRange
import com.google.gson.annotations.SerializedName

class MatchmakerMessage : FafServerMessage(FafServerMessageType.MATCHMAKER_INFO) {

    var action: String? = null
    var queues: List<MatchmakerQueue>? = null

    class MatchmakerQueue(var queueName: String?, @field:SerializedName("boundary_75s")
    var boundary75s: List<RatingRange>?, @field:SerializedName("boundary_80s")
                          var boundary80s: List<RatingRange>?)
}
