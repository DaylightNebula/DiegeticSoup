package dsh.diegetic.interaction

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/** A ray in some coordinate space. [direction] need not be normalised. */
class Ray(val origin: Vector3f, val direction: Vector3f) {
    fun transformed(matrix: Matrix4f) = Ray(
        matrix.transformPosition(Vector3f(origin)),
        matrix.transformDirection(Vector3f(direction))
    )

    companion object {
        /** The ray along a player's view, relative to [relativeTo] and rotated into its local space. */
        @JvmStatic
        fun view(player: DPlayer, relativeTo: DLocation): Ray {
            val location = player.location()
            val yaw = Math.toRadians(location.yaw().toDouble())
            val pitch = Math.toRadians(location.pitch().toDouble())
            val direction = Vector3f(
                (-sin(yaw) * cos(pitch)).toFloat(),
                (-sin(pitch)).toFloat(),
                (cos(yaw) * cos(pitch)).toFloat()
            )
            val origin = Vector3f(location.position()).add(0f, player.eyeHeight(), 0f).sub(relativeTo.position())
            val toLocal = Quaternionf(relativeTo.orientation()).conjugate()
            return Ray(origin.rotate(toLocal), direction.rotate(toLocal))
        }
    }
}

/**
 * The rotation the client applies to a display entity at this location: yaw about the vertical axis
 * (0 faces +z), then pitch about the local x axis.
 */
fun DLocation.orientation(): Quaternionf = Quaternionf().rotationYXZ(
    Math.toRadians(-yaw().toDouble()).toFloat(),
    Math.toRadians(pitch().toDouble()).toFloat(),
    0f
)
