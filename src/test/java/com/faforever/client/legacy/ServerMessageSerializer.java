package com.faforever.client.legacy;

import com.faforever.client.remote.JsonMessageSerializer;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.faforever.client.remote.gson.RatingRangeTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeTypeAdapter;
import com.google.gson.GsonBuilder;

public class ServerMessageSerializer extends JsonMessageSerializer<FafServerMessage> {

  @Override
  protected void addTypeAdapters(GsonBuilder gsonBuilder) {
    super.addTypeAdapters(gsonBuilder);

    gsonBuilder.registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.Companion.getINSTANCE());
    gsonBuilder.registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.Companion.getINSTANCE());
    gsonBuilder.registerTypeAdapter(RatingRange.class, RatingRangeTypeAdapter.Companion.getINSTANCE());
  }
}
