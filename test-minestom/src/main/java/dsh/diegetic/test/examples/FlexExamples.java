package dsh.diegetic.test.examples;

import dsh.diegetic.controller.DSLController;
import dsh.diegetic.flex.AlignItems;
import dsh.diegetic.flex.FlexButton;
import dsh.diegetic.flex.FlexRadioGroup;
import dsh.diegetic.flex.FlexSlider;
import dsh.diegetic.flex.FlexToggle;
import dsh.diegetic.flex.RadioStyle;
import dsh.diegetic.flex.FlexContainer;
import dsh.diegetic.flex.FlexElement;
import dsh.diegetic.flex.FlexItem;
import dsh.diegetic.flex.FlexText;
import dsh.diegetic.flex.JustifyContent;
import dsh.diegetic.flex.Length;
import dsh.diegetic.interop.MinestomItem;
import dsh.diegetic.interop.MinestomLocation;
import dsh.diegetic.interop.MinestomPlayer;
import dsh.diegetic.position.StaticPositionController;
import dsh.diegetic.viewers.StaticViewerController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.List;

/**
 * A shop panel built with the flexbox builders from Java.
 */
public final class FlexExamples {
    private FlexExamples() {}

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    private static FlexContainer card(Material material, String name) {
        return FlexContainer.column()
            .alignItems(AlignItems.CENTER)
            .gap(3)
            .padding(4)
            .grow(1f)
            .background(0x30FFFFFF)
            .child(FlexItem.create(new MinestomItem(ItemStack.of(material))).size(24, 24))
            .child(FlexText.create(mm("<white>" + name)));
    }

    private static FlexContainer button(String label, int color) {
        return FlexContainer.row()
            .justifyContent(JustifyContent.CENTER)
            .padding(3f, 8f)
            .grow(1f)
            .background(color)
            .child(FlexText.create(mm(label)));
    }

    /** The shop panel as a flex tree. */
    public static FlexContainer shopPanel() {
        return FlexContainer.column()
            .width(200)
            .padding(8)
            .gap(6)
            .background(0xE0101820)
            .child(FlexContainer.row()
                .justifyContent(JustifyContent.SPACE_BETWEEN)
                .alignItems(AlignItems.CENTER)
                .child(FlexText.create(mm("<gold><bold>Shop")))
                .child(FlexText.create(mm("<gray>3 items"))))
            .child(FlexText.create(mm("<gray>Pick an item below. This panel is laid out with flexbox and wraps its text to fit.")))
            .child(FlexContainer.row()
                .gap(6)
                .child(card(Material.DIAMOND, "Diamond"))
                .child(card(Material.EMERALD, "Emerald"))
                .child(card(Material.GOLD_INGOT, "Gold")))
            .child(FlexContainer.row()
                .gap(6)
                .child(button("<white>Buy", 0xC0208040))
                .child(button("<white>Cancel", 0xC0902020)));
    }

    /** The shop panel shown to [viewer] at [at], half size. */
    public static DSLController shop(Player viewer, Pos at) {
        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(FlexElement.create(shopPanel()).scale(0.5f));
    }

    /**
     * A settings panel with every interactive widget. Widget state is shared by everyone viewing it;
     * the handlers receive the player who clicked.
     */
    public static FlexContainer settingsPanel() {
        FlexText volumeLabel = FlexText.create(mm("<gray>Volume: 70"));

        return FlexContainer.column()
            .width(180)
            .padding(8)
            .gap(6)
            .background(0xE0101820)
            .child(FlexRadioGroup.<String>create()
                .style(RadioStyle.TABS)
                .option("audio", "Audio")
                .option("video", "Video")
                .option("controls", "Controls")
                .selected("audio")
                .onChange((event, tab) -> event.getPlayer().name()))
            .child(volumeLabel)
            .child(FlexSlider.create(0f, 100f, 70f)
                .step(5f)
                .width(Length.percent(100f))
                .onChange((event, value) -> volumeLabel.text(mm("<gray>Volume: " + Math.round(value)))))
            .child(FlexToggle.create(mm("<white>Subtitles")).checked(true))
            .child(FlexRadioGroup.<String>create()
                .option("low", "Low quality")
                .option("high", "High quality")
                .selected("high"))
            .child(FlexContainer.row()
                .gap(6)
                .child(FlexButton.create(mm("<white>Save")).grow(1f).background(0xC0208040).hoverBackground(0xE030A050))
                .child(FlexButton.create(mm("<white>Reset")).grow(1f)
                    .onLeftClick(event -> System.out.println(event.getPlayer().name() + " reset the settings"))));
    }

    /** The settings panel shown to [viewer] at [at], half size. */
    public static DSLController settings(Player viewer, Pos at) {
        return DSLController.create()
            .viewerController(new StaticViewerController(List.of(new MinestomPlayer(viewer))))
            .positionController(new StaticPositionController(new MinestomLocation(at)))
            .element(FlexElement.create(settingsPanel()).scale(0.5f));
    }
}
