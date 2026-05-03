package dsh.diegetic.interop

import net.minestom.server.entity.Entity

class MinestomEntity(
    val entity: Entity
): DEntity {
    override fun id(): Int = entity.entityId
}