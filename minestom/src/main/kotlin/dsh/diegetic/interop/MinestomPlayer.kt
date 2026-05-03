package dsh.diegetic.interop

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

class MinestomPlayer(
    val player: Player
): DPlayer {

    companion object {
        fun toPlayer(player: DPlayer): Player = MinecraftServer
            .getConnectionManager()
            .getOnlinePlayerByUuid(player.uuid())
                ?: throw IllegalArgumentException("Player is not MinestomPlayer!")
    }

    override fun uuid() = player.uuid
    override fun name() = player.username
    override fun location() = MinestomLocation(player.position)
    override fun eyeHeight() = player.eyeHeight.toFloat()
}