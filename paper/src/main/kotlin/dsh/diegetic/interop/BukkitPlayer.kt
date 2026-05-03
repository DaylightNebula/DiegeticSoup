package dsh.diegetic.interop

import org.bukkit.Bukkit

class BukkitPlayer(
    val bukkitPlayer: org.bukkit.entity.Player
): DPlayer, DEntity {
    companion object {
        fun fromDPlayer(player: DPlayer): BukkitPlayer {
            if (player is BukkitPlayer) return player
            val bukkitPlayer = Bukkit.getPlayer(player.uuid()) ?: throw IllegalArgumentException("Player not found!")
            return BukkitPlayer(bukkitPlayer)
        }
    }

    override fun id() = bukkitPlayer.entityId
    override fun uuid() = bukkitPlayer.uniqueId
    override fun name() = bukkitPlayer.name
    override fun location() = BukkitLocation(bukkitPlayer.location)
    override fun eyeHeight() = bukkitPlayer.eyeHeight.toFloat()
}