package dsh.diegetic.interop

import org.bukkit.inventory.ItemStack

class BukkitItem(
    val bukkitItem: org.bukkit.inventory.ItemStack
): DItem {
    companion object {
        fun toBukkit(item: DItem): ItemStack =
            (item as? BukkitItem)?.bukkitItem
                ?: throw IllegalArgumentException("Item is not BukkitItem!")
    }
}