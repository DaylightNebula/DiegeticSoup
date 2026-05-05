package dsh.diegetic.interop

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack

class BukkitItem(
    val bukkitItem: ItemStack
): DItem {
    companion object {
        fun toItem(item: DItem): ItemStack {
            if (item is BukkitItem) return item.bukkitItem

            // create item stack
            val matKey = NamespacedKey.fromString(item.typeKey())
                ?: throw IllegalArgumentException("Invalid material key ${item.typeKey()}")
            val itemStack = Registry.MATERIAL.get(matKey)?.let { ItemStack(it) }
                ?: throw IllegalArgumentException("Invalid failed to build item from type: ${item.typeKey()}")

            // add custom model data
            item.customModelData()?.let { data ->
                val meta = itemStack.itemMeta
                meta.setCustomModelData(data)
                itemStack.itemMeta = meta
            }

            return itemStack
        }
    }

    override fun typeKey() = bukkitItem.type.key.asString()
    override fun customModelData() = bukkitItem.itemMeta?.customModelData
}