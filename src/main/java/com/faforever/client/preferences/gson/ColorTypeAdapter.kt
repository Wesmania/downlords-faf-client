package com.faforever.client.preferences.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import javafx.scene.paint.Color

import java.io.IOException

class ColorTypeAdapter : TypeAdapter<Color>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Color?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Color {
        return Color.web(`in`.nextString())
    }
}
