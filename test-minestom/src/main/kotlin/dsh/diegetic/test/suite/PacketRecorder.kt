package dsh.diegetic.test.suite

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.network.packet.server.ServerPacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.EntityTeleportPacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Records every packet the server actually sends about diegetic display entities,
 * so scenarios can assert exactly what went over the wire.
 */
object PacketRecorder {
    /** Entity IDs handed out by MinestomDiegeticAPI start here; real entities are far below. */
    private const val FIRST_DISPLAY_ID = Int.MAX_VALUE / 2

    enum class Kind { SPAWN, METADATA, TELEPORT, DESTROY, PASSENGERS }

    data class Record(val kind: Kind, val entityIds: List<Int>, val packet: ServerPacket, val player: Player) {
        /** Value of a metadata entry, for METADATA records. */
        fun metadata(index: Int): Any? = ((packet as EntityMetaDataPacket).entries()[index] as Metadata.Entry<*>?)?.value()
        fun hasMetadata(index: Int) = (packet as EntityMetaDataPacket).entries().containsKey(index)
    }

    private val records = ConcurrentLinkedQueue<Record>()
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketOutEvent::class.java) { event ->
            classify(event.packet, event.player)?.let(records::add)
        }
    }

    private fun classify(packet: ServerPacket, player: Player): Record? {
        val (kind, ids) = when (packet) {
            is SpawnEntityPacket -> Kind.SPAWN to listOf(packet.entityId())
            is EntityMetaDataPacket -> Kind.METADATA to listOf(packet.entityId())
            is EntityTeleportPacket -> Kind.TELEPORT to listOf(packet.entityId())
            is DestroyEntitiesPacket -> Kind.DESTROY to packet.entityIds()
            is SetPassengersPacket -> Kind.PASSENGERS to packet.passengersId()
            else -> return null
        }
        val displayIds = ids.filter { it >= FIRST_DISPLAY_ID }
        if (displayIds.isEmpty()) return null
        return Record(kind, displayIds, packet, player)
    }

    fun clear() = records.clear()
    fun snapshot(): List<Record> = records.toList()

    /** Clears the log, waits [millis], and returns what was sent in that window. */
    fun during(millis: Long): List<Record> {
        clear()
        Thread.sleep(millis)
        return snapshot()
    }
}

fun List<PacketRecorder.Record>.ofKind(kind: PacketRecorder.Kind) = filter { it.kind == kind }

fun List<PacketRecorder.Record>.describe(): String =
    if (isEmpty()) "nothing" else groupingBy { it.kind }.eachCount().entries.joinToString { "${it.value} ${it.key}" }
