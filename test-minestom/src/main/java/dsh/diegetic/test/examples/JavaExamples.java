package dsh.diegetic.test.examples;

import dsh.diegetic.controller.DSLController;
import dsh.diegetic.elements.DSLElement;
import dsh.diegetic.elements.DSLRenderedElement;
import dsh.diegetic.interop.MinestomItem;
import dsh.diegetic.interop.MinestomLocation;
import dsh.diegetic.interop.MinestomPlayer;
import dsh.diegetic.position.DynamicPositionController;
import dsh.diegetic.position.StaticPositionController;
import dsh.diegetic.viewers.StaticViewerController;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.joml.Vector3f;

import java.util.List;

/**
 * Examples of building diegetic UIs from Java with the builder API.
 * Each method returns an unspawned builder; call {@code .spawn()} to show it.
 */
public final class JavaExamples {
    private JavaExamples() {}

    /** An oak plank with a label floating above it. */
    public static DSLController staticItemAndText(Player viewer, Pos at) {
        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(DSLElement.create()
                .draw(DSLRenderedElement.create()
                    .item(new MinestomItem(ItemStack.of(Material.OAK_PLANKS)))
                    .scale(new Vector3f(0.5f)))
                .draw(DSLRenderedElement.create()
                    .text(MiniMessage.miniMessage().deserialize("<green>Java builder"))
                    .translation(new Vector3f(0f, 0.5f, 0f))));
    }

    /** Three items laid out in a row by child elements, with a shared label. */
    public static DSLController nestedChildren(Player viewer, Pos at) {
        Material[] materials = { Material.STONE, Material.DIRT, Material.GOLD_BLOCK };

        DSLElement root = DSLElement.create()
            .draw(DSLRenderedElement.create()
                .text(MiniMessage.miniMessage().deserialize("<yellow>Java children"))
                .translation(new Vector3f(0f, 0.5f, 0f)));

        for (int i = 0; i < materials.length; i++) {
            root.child(DSLElement.create()
                .translate(new Vector3f((i - 1) * 0.6f, 0f, 0f))
                .draw(DSLRenderedElement.create()
                    .item(new MinestomItem(ItemStack.of(materials[i])))
                    .scale(new Vector3f(0.4f))));
        }

        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(root);
    }

    /** An item whose scale pulses over time, driven by a Supplier. */
    public static DSLController pulsingScale(Player viewer, Pos at) {
        long startTime = System.currentTimeMillis();

        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(DSLElement.create()
                .scale(() -> {
                    float diff = (System.currentTimeMillis() - startTime) % 1000 / 1000f;
                    float scale = (float) Math.sin(diff * 2f * Math.PI);
                    return new Vector3f(scale * 0.3f + 0.5f);
                })
                .draw(DSLRenderedElement.create()
                    .item(new MinestomItem(ItemStack.of(Material.DIAMOND_BLOCK)))));
    }

    /** An item whose root position slides side to side, driven by a DynamicPositionController. */
    public static DSLController slidingPosition(Player viewer, Pos center) {
        long startTime = System.currentTimeMillis();

        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new DynamicPositionController(() -> {
                double t = (System.currentTimeMillis() - startTime) / 1000.0;
                return new MinestomLocation(center.add(Math.sin(t * Math.PI) * 1.5, 0.0, 0.0));
            }))
            .element(DSLElement.create()
                .draw(DSLRenderedElement.create()
                    .item(new MinestomItem(ItemStack.of(Material.EMERALD_BLOCK)))
                    .scale(new Vector3f(0.5f))));
    }
}
