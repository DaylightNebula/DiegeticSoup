package dsh.diegetic.demo;

import dsh.diegetic.flex.AlignItems;
import dsh.diegetic.flex.FlexContainer;
import dsh.diegetic.flex.FlexItem;
import dsh.diegetic.flex.FlexText;
import dsh.diegetic.flex.JustifyContent;
import dsh.diegetic.interop.MinestomItem;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/** A shop panel laid out with flexbox, written with the Java builders. */
public final class JavaShopPanel {
    private JavaShopPanel() {}

    private static FlexContainer card(Material material, String name, String price) {
        return FlexContainer.column()
            .alignItems(AlignItems.CENTER)
            .gap(3)
            .padding(4)
            .grow(1f)
            .background(0x30FFFFFF)
            .child(FlexItem.create(new MinestomItem(ItemStack.of(material))).size(24, 24))
            .child(FlexText.create(DemoKt.mm("<white>" + name)))
            .child(FlexText.create(DemoKt.mm("<gold>" + price)));
    }

    public static FlexContainer create() {
        return FlexContainer.column()
            .width(200)
            .padding(8)
            .gap(6)
            .background(0xE0101820)
            .child(FlexContainer.row()
                .justifyContent(JustifyContent.SPACE_BETWEEN)
                .alignItems(AlignItems.CENTER)
                .child(FlexText.create(DemoKt.mm("<gold><bold>Shop")))
                .child(FlexText.create(DemoKt.mm("<gray>Java builders"))))
            .child(FlexText.create(DemoKt.mm("<gray>Cards grow to share the row, and this text wraps to the panel's width.")))
            .child(FlexContainer.row()
                .gap(6)
                .child(card(Material.DIAMOND, "Diamond", "64g"))
                .child(card(Material.EMERALD, "Emerald", "32g"))
                .child(card(Material.GOLD_INGOT, "Gold", "8g")));
    }
}
