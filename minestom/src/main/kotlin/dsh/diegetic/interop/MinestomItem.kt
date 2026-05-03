package dsh.diegetic.interop

import net.minestom.server.item.ItemStack

class MinestomItem(
    val item: ItemStack
): DItem {
    companion object {
        fun toItem(item: DItem): ItemStack =
            (item as? MinestomItem)?.item
                ?: throw IllegalArgumentException("Item is not MinestomItem!")
    }
}