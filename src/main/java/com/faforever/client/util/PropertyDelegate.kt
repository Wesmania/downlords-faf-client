package com.faforever.client.util

import javafx.beans.property.Property
import kotlin.reflect.KProperty

class PropertyDelegate<T>(val wrapped: Property<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = wrapped.value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
         wrapped.value = value
     }
}

/* No automatic conversion to/from number in Kotlin */

class FloatDelegate(val wrapped: Property<Number>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float = wrapped.value.toFloat()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        wrapped.value = value
    }
}

class IntDelegate(val wrapped: Property<Number>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = wrapped.value.toInt()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        wrapped.value = value
    }
}
