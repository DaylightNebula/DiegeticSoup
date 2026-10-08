package dsh.diegetic.demo.stations

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.DSLController
import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.demo.Station
import dsh.diegetic.demo.mm
import dsh.diegetic.demo.show
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.flex.*
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.PlayerPositionController
import dsh.diegetic.viewers.StaticViewerController
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.LinkedList
import java.util.UUID
import kotlin.math.roundToInt

/** A HUD that follows each player's view: one controller per player, updated every tick. */
object HudStation : Station(
    "hud",
    "Follow the player",
    "Per-player UIs that move with your view"
) {
    private val huds = mutableMapOf<UUID, DiegeticController>()

    override fun build(origin: Pos, instance: Instance) {
        show(origin.add(0.0, 2.0, 0.0), flex(scale = 0.6f) {
            direction(FlexDirection.COLUMN); width(170); padding(8); gap(6); background(0xE0101820)
            alignItems(AlignItems.STRETCH)
            text(mm("<white>Each player gets their own controller, positioned in front of their view."))
            button("Toggle my HUD") { event ->
                MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(event.player.uuid())?.let(::toggleHud)
            }.padding(5f, 8f)
        })
    }

    /** Shows or hides [player]'s HUD. */
    fun toggleHud(player: Player) {
        val existing = huds.remove(player.uuid)
        if (existing != null) {
            DiegeticAPI.get().removeController(existing)
            return
        }
        huds[player.uuid] = DSLController.create()
            .viewerController(StaticViewerController(listOf(MinestomPlayer(player))))
            // in front of the eyes, a little right and down
            .positionController(PlayerPositionController(MinestomPlayer(player), Vector3f(-0.55f, -0.3f, 1.2f)))
            .element(HudElement(player))
            .spawn()
    }

    /** Coordinates and heading, refreshed before each render, turned to face the player. */
    private class HudElement(private val player: Player) : DiegeticElement {
        private val coordinates = FlexText.create("")
        private val heading = FlexText.create("")
        private val panel = FlexElement.create(
            FlexContainer.column().padding(4).gap(2).background(0xB0101820)
                .child(FlexText.create(mm("<gold><bold>HUD")))
                .child(coordinates)
                .child(heading)
        ).scale(0.25f)

        override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) = render(output, parent, RenderContext.NONE)

        override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f, context: RenderContext) {
            val pos = player.position
            coordinates.text(mm("<white>${pos.blockX()} ${pos.blockY()} ${pos.blockZ()}"))
            heading.text(mm("<gray>Facing ${compass(pos.yaw)}"))
            // the controller turns the panel with the player's view, so its front faces away: turn it round
            panel.render(output, Matrix4f(parent).rotateY(Math.PI.toFloat()), context)
        }

        private fun compass(yaw: Float): String {
            val directions = listOf("south", "west", "north", "east")
            return directions[(((yaw % 360 + 360) % 360) / 90f).roundToInt() % 4]
        }
    }
}
