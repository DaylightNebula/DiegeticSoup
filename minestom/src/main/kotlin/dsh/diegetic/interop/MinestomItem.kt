package dsh.diegetic.interop

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.CustomModelData
import kotlin.math.roundToInt

class MinestomItem(
    val item: ItemStack
): DItem {
    companion object {
        fun toItem(item: DItem): ItemStack {
            if (item is MinestomItem) return item.item

            var itemStack = ItemStack.of(Material.fromKey(item.typeKey()))
            item.customModelData()?.let { data ->
                itemStack = itemStack.with(
                    DataComponents.CUSTOM_MODEL_DATA,
                    CustomModelData(
                        listOf(data.toFloat()),
                        emptyList(),
                        emptyList(),
                        emptyList()
                    )
                )
            }
            return itemStack
        }
    }

    override fun typeKey() = item.material().key().asString()
    override fun customModelData() = item.get(DataComponents.CUSTOM_MODEL_DATA)?.floats?.first()?.toInt()
}