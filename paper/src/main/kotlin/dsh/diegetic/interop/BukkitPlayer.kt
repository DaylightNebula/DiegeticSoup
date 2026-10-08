package dsh.diegetic.interop

import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute

class BukkitPlayer(
    val bukkitPlayer: org.bukkit.entity.Player
): DPlayer(), DEntity {
    companion object {
        fun fromDPlayer(player: DPlayer): BukkitPlayer {
            if (player is BukkitPlayer) return player
            val bukkitPlayer = Bukkit.getPlayer(player.uuid()) ?: throw IllegalArgumentException("Player not found!")
            return BukkitPlayer(bukkitPlayer)
        }
    }

    override fun id() = bukkitPlayer.entityId

    /** Paper applies creative mode's reach bonus as an attribute modifier, so the value already includes it. */
    override fun interactionRange(): Float =
        bukkitPlayer.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value?.toFloat() ?: DEFAULT_INTERACTION_RANGE
    override fun uuid() = bukkitPlayer.uniqueId
    override fun name() = bukkitPlayer.name
    override fun location() = BukkitLocation(bukkitPlayer.location)
    override fun eyeHeight() = bukkitPlayer.eyeHeight.toFloat()
}