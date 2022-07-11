package io.pelmenstar.onealarm.shared.compose

import androidx.compose.runtime.Stable

/**
 * Special class which holds an array and marked as stable. Allows to enable some Compose optimisations for arrays.
 */
@Suppress("NOTHING_TO_INLINE")
@Stable
class StableArray<T> private constructor(@PublishedApi internal val data: Array<out T>): Collection<T> {
    override val size: Int
        get() = data.size

    inline operator fun get(index: Int): T = data[index]

    inline fun forEachIndexed(block: (index: Int, value: T) -> Unit) = data.forEachIndexed(block)
    inline fun forEach(block: (value: T) -> Unit) = data.forEach(block)

    override fun isEmpty(): Boolean = size == 0
    override fun contains(element: T): Boolean = data.contains(element)
    override fun containsAll(elements: Collection<T>) = elements.all(this::contains)

    override fun equals(other: Any?): Boolean {
        if(other === this) return true
        if(other == null || other.javaClass == javaClass) return false

        other as StableArray<*>

        return data.contentEquals(other.data)
    }

    override fun hashCode() = data.contentHashCode()
    override fun iterator() = data.iterator()

    companion object {
        fun<T> wrap(data: Array<out T>) = StableArray(data)
    }
}