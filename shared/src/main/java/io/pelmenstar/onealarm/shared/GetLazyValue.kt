package io.pelmenstar.onealarm.shared

/**
 * If [currentValue] is not null, returns it. Otherwise creates value [T] by [create] lambda and sets the value by [set] lambda.
 * Pretty useful for lazy initializing
 */
inline fun<T : Any> getLazyValue(currentValue: T?, create: () -> T, set: (T) -> Unit): T {
    currentValue?.let { return it }

    return create().also(set)
}