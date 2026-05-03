package dsh.diegetic.interop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.joml.Vector3f

class BukkitLocation(
    val bukkitLocation: org.bukkit.Location
): DLocation {

    companion object {
        fun fromDLocation(location: DLocation): BukkitLocation {
            if (location is BukkitLocation) return location
            val world = Bukkit.getWorld(location.world()) ?: throw IllegalArgumentException("World not found!")
            return BukkitLocation(Location(
                world,
                location.position().x.toDouble(),
                location.position().y.toDouble(),
                location.position().z.toDouble(),
                location.yaw(),
                location.pitch()
            ))
        }
    }

    override fun world() = bukkitLocation.world.name
    override fun position() = Vector3f(bukkitLocation.x.toFloat(), bukkitLocation.y.toFloat(), bukkitLocation.z.toFloat())
    override fun yaw() = bukkitLocation.yaw
    override fun pitch() = bukkitLocation.pitch
}