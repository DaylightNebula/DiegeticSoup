package dsh.diegetic.position

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import org.joml.Quaternionf
import org.joml.Vector3f

class PlayerPositionController(
    val player: DPlayer,
    val offset: Vector3f,
    val yawOverride: Float? = null,
    val pitchOverride: Float? = null,
    val useHeadLocation: Boolean = true
): PositionController {
    override fun getPosition(): DLocation {
        // calculate rotation
        val yaw = yawOverride ?: player.location().yaw()
        val pitch = pitchOverride ?: player.location().pitch()
        val rotation = Quaternionf()
        rotation.rotateYXZ(
            Math.toRadians(-yaw.toDouble()).toFloat(),
            Math.toRadians(pitch.toDouble()).toFloat(),
            0f
        )

        // calculate location
        val location = player.location()
        val offset = Vector3f(this.offset).rotate(rotation)
        var position = location.position()
        if (useHeadLocation) position = position.add(0f, player.eyeHeight(), 0f)
        return DLocation.StaticLocation(
            location.world(),
            position.add(offset),
            location.yaw(),
            location.pitch()
        )
    }
}