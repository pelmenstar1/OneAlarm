package io.pelmenstar.onealarm.shared

import androidx.compose.runtime.Stable

object ShortPair {
    @Stable
    fun create(first: Short, second: Short): Int {
        return (second.toInt() shl 16) or (first.toInt() and 0xFFFF)
    }

    @Stable
    fun getFirst(packed: Int): Short {
        return (packed and 0xFFFF).toShort()
    }

    @Stable
    fun getSecond(packed: Int): Short {
        return (packed shr 16).toShort()
    }
}

object IntPair {
    @Stable
    fun create(first: Int, second: Int): Long {
        return (second.toLong() shl 32) or (first.toLong() and 0xFFFFFFFF)
    }

    @Stable
    fun getFirst(packed: Long): Int {
        return packed.toInt()
    }

    @Stable
    fun getSecond(packed: Long): Int {
        return (packed shr 32).toInt()
    }
}

object FloatPair {
    @Stable
    fun create(first: Float, second: Float) = IntPair.create(first.toBits(), second.toBits())

    @Stable
    fun getFirst(packed: Long) = Float.fromBits(IntPair.getFirst(packed))

    @Stable
    fun getSecond(packed: Long) = Float.fromBits(IntPair.getSecond(packed))
}