package dsh.diegetic.interop

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import java.util.UUID

class MinestomPlayer(
    val player: Player
): DPlayer() {

    companion object {
        fun toPlayer(player: DPlayer): Player = MinecraftServer
            .getConnectionManager()
            .getOnlinePlayerByUuid(player.uuid())
                ?: throw IllegalArgumentException("Player is not MinestomPlayer!")
    }

    override fun uuid(): UUID = player.uuid
    override fun name(): String = player.username
    override fun location() = MinestomLocation(player.position)
    override fun eyeHeight() = player.eyeHeight.toFloat()

    /**
     * The attribute value, which the client uses as its reach too. Vanilla servers give creative players
     * 2 extra blocks through an attribute modifier; Minestom doesn't add it, so creative reach here is
     * the same as survival unless the server sets the attribute.
     */
    override fun interactionRange(): Float = player.getAttributeValue(Attribute.ENTITY_INTERACTION_RANGE).toFloat()
}