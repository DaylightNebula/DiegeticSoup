package dsh.diegetic.demo;

import dsh.diegetic.controller.DSLController;
import dsh.diegetic.controller.DiegeticController;
import dsh.diegetic.elements.DSLElement;
import dsh.diegetic.elements.DSLRenderedElement;
import dsh.diegetic.interop.MinestomItem;
import dsh.diegetic.interop.MinestomLocation;
import dsh.diegetic.position.StaticPositionController;
import dsh.diegetic.viewers.InRadiusViewerController;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The basics station's right-hand exhibit, built with the Java builders: a bobbing, spinning emerald
 * block with a label. Compare with the Kotlin DSL version in BasicsStation.
 */
public final class JavaBasics {
    private JavaBasics() {}

    public static DiegeticController spinningEmerald(Pos at) {
        long start = System.currentTimeMillis();

        DSLElement spinner = DSLElement.create()
            .translate(() -> new Vector3f(0f, (float) Math.sin((System.currentTimeMillis() - start) / 600.0) * 0.08f, 0f))
            .rotation(() -> new Quaternionf().rotateY((System.currentTimeMillis() - start) / 900f))
            .draw(DSLRenderedElement.create()
                .item(new MinestomItem(ItemStack.of(Material.EMERALD_BLOCK)))
                .scale(new Vector3f(0.6f)));

        DSLElement label = DSLElement.create()
            .translate(new Vector3f(0f, 0.75f, 0f))
            .draw(DSLRenderedElement.create()
                .text(DemoKt.mm("<green>Java builders"))
                .scale(new Vector3f(0.8f)));

        return DSLController.create()
            .viewerController(new InRadiusViewerController(new MinestomLocation(at), 48.0))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(DSLElement.create().child(spinner).child(label))
            .spawn();
    }
}
