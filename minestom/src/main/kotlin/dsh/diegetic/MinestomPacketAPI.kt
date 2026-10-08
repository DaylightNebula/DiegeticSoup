package dsh.diegetic

import dsh.diegetic.elements.TextDisplayOptions
import dsh.diegetic.interop.DItem
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Metadata
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.EntityTeleportPacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

class MinestomPacketAPI: PacketAPI {
    /** The viewer's player, or null once they have left: packets to departed viewers are dropped. */
    private fun online(viewer: DPlayer): Player? =
        MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(viewer.uuid())

    override fun spawnItemDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        item: DItem,
        location: DLocation,
        offset: Matrix4f,
        teleportDuration: Int
    ) {
        // create spawn packet
        val spawnPacket = SpawnEntityPacket(
            entityId, UUID.randomUUID(),
            EntityType.ITEM_DISPLAY,
            MinestomLocation.toPos(location),
            0f, 0, Vec(0.0, 0.0, 0.0)
        )

        // create metadata packet
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getUnnormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.VarInt(0),
                9 to Metadata.VarInt(0),
                10 to Metadata.VarInt(teleportDuration),
                11 to Metadata.Vector3(Vec(translation.x.toDouble(), translation.y.toDouble(), translation.z.toDouble())),
                12 to Metadata.Vector3(Vec(scale.x.toDouble(), scale.y.toDouble(), scale.z.toDouble())),
                13 to Metadata.Quaternion(floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w)),
                23 to Metadata.ItemStack(MinestomItem.toItem(item))
            )
        )

        // send to viewers
        viewers.forEach { viewer ->
            val player = online(viewer) ?: return@forEach
            player.sendPacket(spawnPacket)
            player.sendPacket(metadataPacket)
        }
    }

    override fun spawnTextDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        text: Component,
        location: DLocation,
        offset: Matrix4f,
        teleportDuration: Int,
        options: TextDisplayOptions
    ) {
        // create spawn packet
        val spawnPacket = SpawnEntityPacket(
            entityId, UUID.randomUUID(),
            EntityType.TEXT_DISPLAY,
            MinestomLocation.toPos(location),
            0f, 0, Vec(0.0, 0.0, 0.0)
        )

        // create metadata packet
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getUnnormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.VarInt(0),
                9 to Metadata.VarInt(0),
                10 to Metadata.VarInt(teleportDuration),
                11 to Metadata.Vector3(Vec(translation.x.toDouble(), translation.y.toDouble(), translation.z.toDouble())),
                12 to Metadata.Vector3(Vec(scale.x.toDouble(), scale.y.toDouble(), scale.z.toDouble())),
                13 to Metadata.Quaternion(floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w)),
                23 to Metadata.Component(text),
                24 to Metadata.VarInt(options.lineWidth),
                25 to Metadata.VarInt(options.resolvedBackground),
                27 to Metadata.Byte(options.alignment.flags)
            )
        )
        // send to viewers
        viewers.forEach { viewer ->
            val player = online(viewer) ?: return@forEach
            player.sendPacket(spawnPacket)
            player.sendPacket(metadataPacket)
        }
    }

    override fun updateDisplayOffset(
        viewers: Collection<DPlayer>,
        entityId: Int,
        offset: Matrix4f,
        interpolationDuration: Int
    ) {
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getUnnormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.VarInt(0),
                9 to Metadata.VarInt(interpolationDuration),
                11 to Metadata.Vector3(Vec(translation.x.toDouble(), translation.y.toDouble(), translation.z.toDouble())),
                12 to Metadata.Vector3(Vec(scale.x.toDouble(), scale.y.toDouble(), scale.z.toDouble())),
                13 to Metadata.Quaternion(floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w)),
            )
        )
        viewers.forEach { online(it)?.sendPacket(metadataPacket) }
    }

    override fun updateItemDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        item: DItem
    ) {
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                23 to Metadata.ItemStack(MinestomItem.toItem(item))
            )
        )
        viewers.forEach { online(it)?.sendPacket(metadataPacket) }
    }

    override fun updateTextDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        text: Component,
        options: TextDisplayOptions
    ) {
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                23 to Metadata.Component(text),
                24 to Metadata.VarInt(options.lineWidth),
                25 to Metadata.VarInt(options.resolvedBackground),
                27 to Metadata.Byte(options.alignment.flags)
            )
        )
        viewers.forEach { online(it)?.sendPacket(metadataPacket) }
    }

    override fun moveEntity(
        viewers: Collection<DPlayer>,
        entityId: Int,
        location: DLocation
    ) {
        val teleport = EntityTeleportPacket(
            entityId,
            MinestomLocation.toPos(location),
            Vec(0.0, 0.0, 0.0),
            0, false
        )
        viewers.forEach { online(it)?.sendPacket(teleport) }
    }

    override fun scaleEntity(
        viewers: Collection<DPlayer>,
        entityId: Int,
        scale: Float
    ) {
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                12 to Metadata.Vector3(Vec(scale.toDouble(), scale.toDouble(), scale.toDouble())),
            )
        )
        viewers.forEach { online(it)?.sendPacket(metadataPacket) }
    }

    override fun removeEntity(
        viewers: Collection<DPlayer>,
        entityId: Int
    ) {
        val removePacket = DestroyEntitiesPacket(entityId)
        viewers.forEach { online(it)?.sendPacket(removePacket) }
    }

    override fun removeEntities(
        viewers: Collection<DPlayer>,
        entityIds: Collection<Int>
    ) {
        val removePacket = DestroyEntitiesPacket(entityIds.toList())
        viewers.forEach { online(it)?.sendPacket(removePacket) }
    }

    override fun setEntityPassengers(
        viewers: Collection<DPlayer>,
        parentId: Int,
        entityIds: Collection<Int>
    ) {
        val packet = SetPassengersPacket(parentId, entityIds.toList())
        viewers.forEach { online(it)?.sendPacket(packet) }
    }

    override fun spawnInteraction(
        viewers: Collection<DPlayer>,
        entityId: Int,
        location: DLocation,
        width: Float,
        height: Float
    ) {
        val spawnPacket = SpawnEntityPacket(
            entityId, UUID.randomUUID(),
            EntityType.INTERACTION,
            MinestomLocation.toPos(location),
            0f, 0, Vec(0.0, 0.0, 0.0)
        )
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.Float(width),
                9 to Metadata.Float(height),
                10 to Metadata.Boolean(true)
            )
        )
        viewers.forEach { viewer ->
            val player = online(viewer) ?: return@forEach
            player.sendPacket(spawnPacket)
            player.sendPacket(metadataPacket)
        }
    }

    override fun updateInteractionSize(
        viewers: Collection<DPlayer>,
        entityId: Int,
        width: Float,
        height: Float
    ) {
        val metadataPacket = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.Float(width),
                9 to Metadata.Float(height)
            )
        )
        viewers.forEach { online(it)?.sendPacket(metadataPacket) }
    }
}
