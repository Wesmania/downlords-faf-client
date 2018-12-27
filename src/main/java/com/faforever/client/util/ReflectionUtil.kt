package com.faforever.client.util

object ReflectionUtil {

    /**
     * Searches for the Field of name {@param fieldName} in the {@param targetClass} and its super classes and return the
     * type of that field.
     *
     * @param fieldName name of the field
     * @param targetClass The class to look in
     * @return The class type of the field
     * @throws NoSuchFieldException When no field is found
     */
    @Throws(NoSuchFieldException::class)
    fun getDeclaredField(fieldName: String, targetClass: Class<*>): Class<*> {
        var currentClass: Class<*>? = targetClass
        var clazz: Class<*>? = null
        while (clazz == null) {
            try {
                clazz = currentClass!!.getDeclaredField(fieldName).type
            } catch (e: NoSuchFieldException) {
                currentClass = targetClass.superclass
                if (currentClass == null) {
                    throw NoSuchFieldException(fieldName)
                }
            }

        }
        return clazz
    }
}
