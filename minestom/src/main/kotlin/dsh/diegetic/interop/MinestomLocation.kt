package dsh.diegetic.interop

import net.minestom.server.coordinate.Pos
import org.joml.Vector3f

class MinestomLocation(
    val pos: Pos
): DLocation {

    companion object {
        fun toPos(loc: DLocation): Pos {
            val pos = loc.position()
            return Pos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), loc.yaw(), loc.pitch())
        }
    }

    override fun world() = ""
    override fun position() = Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
    override fun yaw() = pos.yaw
    override fun pitch() = pos.pitch
}