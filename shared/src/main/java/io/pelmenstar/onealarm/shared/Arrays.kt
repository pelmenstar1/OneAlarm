@file:Suppress("UNCHECKED_CAST")
package io.pelmenstar.onealarm.shared

inline fun<reified T> Array<out T>.withAdded(element: T): Array<T> {
    val newArray = arrayOfNulls<T>(size + 1)
    System.arraycopy(this, 0, newArray, 0, size)
    newArray[size] = element

    return newArray as Array<T>
}

inline fun<reified T> Array<out T>.withRemovedAt(index: Int): Array<T> {
    val newArray = arrayOfNulls<T>(size - 1)

    System.arraycopy(this, 0, newArray, 0, index)
    System.arraycopy(this, index + 1, newArray, index, size - (index + 1))

    return newArray as Array<T>
}

inline fun<reified T> Array<T>.withRemoved(element: T): Array<T> {
    return indexOf(element).let { if(it >= 0) withRemovedAt(it) else this }
}