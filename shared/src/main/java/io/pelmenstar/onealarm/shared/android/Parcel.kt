package io.pelmenstar.onealarm.shared.android

import android.os.Parcel

/**
 * Reads a string from the [Parcel] at its current position.
 * If the string is null, throws an [IllegalStateException]
 */
fun Parcel.readStringNotNull() = readString() ?: throw IllegalStateException("String supposed to be not-null is null")

/**
 * Reads a typed object [T] from the [Parcel] at its current position.
 * If the object is null, throws an [IllegalStateException]
 */
@Suppress("UNCHECKED_CAST")
fun<T> Parcel.readValueNotNull(loader: ClassLoader?) = requireNotNull(readValue(loader)) as T