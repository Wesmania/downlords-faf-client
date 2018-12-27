package com.faforever.client.preferences.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SetProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleSetProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.WritableObjectValue
import javafx.collections.FXCollections

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class PropertyTypeAdapter private constructor() : JsonSerializer<Property<*>>, JsonDeserializer<Property<*>> {

    override fun serialize(src: Property<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        if (src.value == null) {
            return JsonNull.INSTANCE
        }
        if (src is StringProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is IntegerProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is DoubleProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is LongProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is FloatProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is BooleanProperty) {
            return JsonPrimitive(src.get())
        }
        if (src is WritableObjectValue<*>) {
            return context.serialize((src as WritableObjectValue<*>).get())
        }

        throw IllegalStateException("Unhandled object type: " + src.javaClass)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Property<*> {
        if (typeOfT is Class<*>) {
            val clazz = typeOfT
            if (StringProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleStringProperty(json.asString)
            }
            if (IntegerProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleIntegerProperty(json.asInt)
            }
            if (DoubleProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleDoubleProperty(json.asDouble)
            }
            if (LongProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleLongProperty(json.asLong)
            }
            if (FloatProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleFloatProperty(json.asFloat)
            }
            if (BooleanProperty::class.java.isAssignableFrom(clazz)) {
                return SimpleBooleanProperty(json.asBoolean)
            }
            if (SetProperty<*>::class.java.isAssignableFrom(clazz)) {
                return SimpleSetProperty<Any>(context.deserialize<ObservableSet<Any>>(json, Set<*>::class.java))
            }
            if (MapProperty<*, *>::class.java.isAssignableFrom(clazz)) {
                return SimpleMapProperty<Any, Any>(context.deserialize<ObservableMap<Any, Any>>(json, Map<*, *>::class.java))
            }
        }
        if (typeOfT is ParameterizedType) {
            val parameterizedType = typeOfT
            val rawType = parameterizedType.rawType

            if (rawType === ObjectProperty<*>::class.java) {
                return SimpleObjectProperty(context.deserialize<Any>(json, parameterizedType.actualTypeArguments[0]))
            } else if (rawType === ListProperty<*>::class.java) {
                val type = CustomType(List<*>::class.java, parameterizedType.actualTypeArguments)
                return SimpleListProperty(FXCollections.observableList(context.deserialize<List<Any>>(json, type)))
            } else if (rawType === SetProperty<*>::class.java) {
                val type = CustomType(Set<*>::class.java, parameterizedType.actualTypeArguments)
                // Why is this the only call that needs parameterization?
                return SimpleSetProperty(FXCollections.observableSet(context.deserialize<Set<Any>>(json, type)))
            } else if (rawType === MapProperty<*, *>::class.java) {
                val type = CustomType(Map<*, *>::class.java, parameterizedType.actualTypeArguments)
                return SimpleMapProperty(FXCollections.observableMap(context.deserialize<Map<Any, Any>>(json, type)))
            }
        }

        throw IllegalStateException("Unhandled object type: $typeOfT")
    }

    private inner class CustomType(private val rawType: Class<*>, private val typeArguments: Array<Type>) : ParameterizedType {

        override fun getActualTypeArguments(): Array<Type> {
            return typeArguments
        }

        override fun getRawType(): Type {
            return rawType
        }

        override fun getOwnerType(): Type? {
            return null
        }
    }

    companion object {

        val INSTANCE = PropertyTypeAdapter()
    }
}
