package dsh.diegetic.interop

import org.joml.Vector3f

interface DLocation {
    fun world(): String         // world this location is in
    fun position(): Vector3f    // position in meters from world origin
    fun yaw(): Float            // yaw degrees
    fun pitch(): Float          // pitch degrees

    fun distanceTo(other: DLocation): Float {
        if (world() != other.world()) return Float.POSITIVE_INFINITY
        return position().distance(Vector3f(other.position()))
    }

    fun distanceToSq(other: DLocation): Float {
        if (world() != other.world()) return Float.POSITIVE_INFINITY
        return position().distanceSquared(Vector3f(other.position()))
    }

    data class StaticLocation(
        val world: String,
        val position: Vector3f,
        val yaw: Float,
        val pitch: Float
    ): DLocation {
        override fun world() = world
        override fun position() = position
        override fun yaw() = yaw
        override fun pitch() = pitch
    }
}
