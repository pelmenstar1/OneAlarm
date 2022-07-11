import kotlin.math.cos
import kotlin.math.sin

const val ANGLE_PER_TICK = Math.PI * 2 / 120

object IntPair {
    fun create(first: Int, second: Int): Long {
        return (second.toLong() shl 32) or (first.toLong() and 0xFFFFFFFF)
    }

    fun getFirst(packed: Long): Int {
        return packed.toInt()
    }

    fun getSecond(packed: Long): Int {
        return (packed shr 32).toInt()
    }
}

object FloatPair {
    fun create(first: Float, second: Float) = IntPair.create(first.toBits(), second.toBits())
    fun getFirst(packed: Long) = Float.fromBits(IntPair.getFirst(packed))
    fun getSecond(packed: Long) = Float.fromBits(IntPair.getSecond(packed))
}

fun main() {
    for (i in 0..120) {
        val angle = i * ANGLE_PER_TICK
        val sin = sin(angle).toFloat()
        val cos = cos(angle).toFloat()
        val packed = FloatPair.create(sin, cos)

        println("${packed}L,")
    }
}