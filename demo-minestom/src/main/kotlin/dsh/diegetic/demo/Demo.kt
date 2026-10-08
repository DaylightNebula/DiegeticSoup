package dsh.diegetic.demo

import dsh.diegetic.controller.DSLController
import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.flex.Anchor
import dsh.diegetic.flex.FlexContainer
import dsh.diegetic.flex.FlexElement
import dsh.diegetic.flex.FlexText
import dsh.diegetic.elements.TextAlignment
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.InRadiusViewerController
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

/** Parses MiniMessage, e.g. `mm("<gold>Shop")`. */
fun mm(text: String): Component = MiniMessage.miniMessage().deserialize(text)

/** Everyone within [radius] blocks of [center] sees the UI. */
fun nearby(center: Pos, radius: Double = 48.0) = InRadiusViewerController(MinestomLocation(center), radius)

/** Shows [element] at [at] to everyone nearby. */
fun show(at: Pos, element: DiegeticElement, configure: DSLController.() -> Unit = {}): DiegeticController =
    DSLController.create()
        .viewerController(nearby(at))
        .positionController(StaticPositionController(MinestomLocation(at)))
        .element(element)
        .apply(configure)
        .spawn()

/**
 * One area of the demo world. Stations are built facing south (+z), so players look at them from the
 * south, standing on the platform's front edge.
 */
abstract class Station(val id: String, val title: String, val subtitle: String) {
    /** Builds the station around [origin], the centre of its platform at ground level. */
    abstract fun build(origin: Pos, instance: Instance)

    /** Where a screenshot of this station is taken from, relative to its origin. */
    open val cameraEye: Vec = Vec(0.0, 2.6, 4.8)
    /** What the screenshot looks at, relative to the origin. */
    open val cameraTarget: Vec = Vec(0.0, 2.3, 0.0)

    /** A platform to stand on and a title above the station. */
    fun buildSurroundings(origin: Pos, instance: Instance) {
        for (x in -6..6) for (z in -4..4) {
            val edge = x == -6 || x == 6 || z == -4 || z == 4
            instance.setBlock(origin.blockX() + x, origin.blockY() - 1, origin.blockZ() + z,
                if (edge) Block.POLISHED_DEEPSLATE else Block.POLISHED_ANDESITE)
        }
        val heading = FlexContainer.column()
            .alignItems(dsh.diegetic.flex.AlignItems.CENTER)
            .gap(2)
            .padding(5f, 8f)
            .background(0xE0101820)
            .child(FlexText.create(mm("<bold><gradient:#7ee8fa:#80ff72>$title")).textAlign(TextAlignment.CENTER))
            .child(FlexText.create(mm("<gray>$subtitle")).textAlign(TextAlignment.CENTER).maxWidth(240f))
        show(origin.add(0.0, 4.0, 0.0), FlexElement.create(heading).anchor(Anchor.BOTTOM_CENTER).scale(0.9f))
    }
}

/** Every station, in the order they stand in the hub from west to east. */
val stations: List<Station> = listOf(
    dsh.diegetic.demo.stations.BasicsStation,
    dsh.diegetic.demo.stations.FlexboxStation,
    dsh.diegetic.demo.stations.WidgetsStation,
    dsh.diegetic.demo.stations.HudStation,
    dsh.diegetic.demo.stations.AnimationStation,
)

/** Distance between neighbouring stations in the hub. */
const val STATION_SPACING = 14
