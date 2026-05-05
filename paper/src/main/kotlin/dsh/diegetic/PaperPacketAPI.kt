package dsh.diegetic

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import dsh.diegetic.interop.BukkitItem
import dsh.diegetic.interop.BukkitLocation
import dsh.diegetic.interop.BukkitPlayer
import dsh.diegetic.interop.DItem
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import org.bukkit.entity.EntityType
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

class PaperPacketAPI: PacketAPI {
    override fun spawnItemDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        item: DItem,
        location: DLocation,
        offset: Matrix4f
    ) {
        // create spawn packet
        val spawnPacket = WrapperPlayServerSpawnEntity(
            entityId, UUID.randomUUID(),
            SpigotConversionUtil.fromBukkitEntityType(EntityType.ITEM_DISPLAY),
            SpigotConversionUtil.fromBukkitLocation(BukkitLocation.fromDLocation(location).bukkitLocation),
            location.yaw(),
            0,
            null
        )

        // create metadata packet
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getNormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(8, EntityDataTypes.INT, 1),
                EntityData(9, EntityDataTypes.INT, 1),
                EntityData(10, EntityDataTypes.INT, 1),
                EntityData(23, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(BukkitItem.toItem(item))),
                EntityData(11, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z)),
                EntityData(12, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)),
                EntityData(13, EntityDataTypes.QUATERNION, Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w))
            )
        )

        // send packets
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, spawnPacket)
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun spawnTextDisplay(
        viewers: Collection<DPlayer>,
        entityId: Int,
        text: Component,
        location: DLocation,
        offset: Matrix4f
    ) {
        // create spawn packet
        val spawnPacket = WrapperPlayServerSpawnEntity(
            entityId, UUID.randomUUID(),
            SpigotConversionUtil.fromBukkitEntityType(EntityType.TEXT_DISPLAY),
            SpigotConversionUtil.fromBukkitLocation(BukkitLocation.fromDLocation(location).bukkitLocation),
            location.yaw(),
            0,
            null
        )

        // create metadata packet
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getNormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(8, EntityDataTypes.INT, 1),
                EntityData(9, EntityDataTypes.INT, 1),
                EntityData(10, EntityDataTypes.INT, 1),
                EntityData(23, EntityDataTypes.ADV_COMPONENT, text),
                EntityData(11, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z)),
                EntityData(12, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)),
                EntityData(13, EntityDataTypes.QUATERNION, Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w))
            )
        )

        // send packets to players
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, spawnPacket)
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun updateDisplayOffset(viewers: Collection<DPlayer>, entityId: Int, offset: Matrix4f) {
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()
        offset.getTranslation(translation)
        offset.getNormalizedRotation(rotation)
        offset.getScale(scale)
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(8, EntityDataTypes.INT, 1),
                EntityData(9, EntityDataTypes.INT, 1),
                EntityData(10, EntityDataTypes.INT, 1),
                EntityData(11, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z)),
                EntityData(12, EntityDataTypes.VECTOR3F, com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)),
                EntityData(13, EntityDataTypes.QUATERNION, Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w))
            )
        )
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun updateItemDisplay(viewers: Collection<DPlayer>, entityId: Int, item: DItem) {
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(23, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(BukkitItem.toItem(item))),
            )
        )
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun updateTextDisplay(viewers: Collection<DPlayer>, entityId: Int, text: Component) {
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(23, EntityDataTypes.ADV_COMPONENT, text),
            )
        )
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun moveEntity(viewers: Collection<DPlayer>, entityId: Int, location: DLocation) {
        val packet = WrapperPlayServerEntityTeleport(entityId, SpigotConversionUtil.fromBukkitLocation(BukkitLocation.fromDLocation(location).bukkitLocation), false)
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, packet)
        }
    }

    override fun scaleEntity(viewers: Collection<DPlayer>, entityId: Int, scale: Float) {
        val metadataPacket = WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(
                    12,
                    EntityDataTypes.VECTOR3F,
                    com.github.retrooper.packetevents.util.Vector3f(scale, scale, scale)
                ),
            )
        )
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, metadataPacket)
        }
    }

    override fun removeEntity(viewers: Collection<DPlayer>, entityId: Int) {
        val packet = WrapperPlayServerDestroyEntities(entityId)
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, packet)
        }
    }

    override fun removeEntities(viewers: Collection<DPlayer>, entityIds: Collection<Int>) {
        val packet = WrapperPlayServerDestroyEntities(*entityIds.toIntArray())
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, packet)
        }
    }

    override fun setEntityPassengers(viewers: Collection<DPlayer>, parentId: Int, entityIds: Collection<Int>) {
        val packet = WrapperPlayServerSetPassengers(parentId, entityIds.toIntArray())
        viewers.forEach { player ->
            PacketEvents.getAPI().playerManager.sendPacket(BukkitPlayer.fromDPlayer(player).bukkitPlayer, packet)
        }
    }
}