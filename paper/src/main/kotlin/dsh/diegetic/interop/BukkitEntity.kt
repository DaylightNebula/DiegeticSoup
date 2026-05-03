package dsh.diegetic.interop

class BukkitEntity(
    val bukkitEntity: org.bukkit.entity.Entity
): DEntity {
    override fun id(): Int = bukkitEntity.entityId
}