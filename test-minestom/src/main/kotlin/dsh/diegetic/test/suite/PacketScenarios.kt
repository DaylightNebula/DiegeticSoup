package dsh.diegetic.test.suite

import dsh.diegetic.controller.*
import dsh.diegetic.elements.*
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.DynamicPositionController
import dsh.diegetic.test.examples.JavaExamples
import dsh.diegetic.test.examples.KotlinExamples
import dsh.diegetic.test.suite.PacketRecorder.Kind
import dsh.diegetic.viewers.StaticViewerController
import dsh.nebsclient.api.MoveDirection
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

/** A test element that renders its child only while [visible] is true. */
class ToggleElement(private val child: DiegeticElement): DiegeticElement {
    @Volatile var visible = true
    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        if (visible) child.render(output, parent)
    }
}

val packetScenarios = listOf(

    Scenario("interpolation-settings-are-validated", needsClient = false) {
        val base = { DSLController.create()
            .viewerController(StaticViewerController(emptyList()))
            .positionController(dsh.diegetic.position.StaticPositionController(MinestomLocation(ANCHOR)))
            .element(DSLElement.create()) }

        expectThrows<IllegalArgumentException>("teleportDuration above 59") { base().teleportDuration(60).build() }
        expectThrows<IllegalArgumentException>("negative interpolationDuration") { base().interpolationDuration(-1).build() }

        val built = base().interpolationDuration(4).teleportDuration(7).build()
        check(built.interpolationDuration == 4 && built.teleportDuration == 7) { "Durations were not passed to the controller" }

        val defaults = base().build()
        check(defaults.interpolationDuration == 1 && defaults.teleportDuration == 1) { "Defaults should be 1 tick" }

        val dsl = DSLController.create().apply { interpolationDuration = 2; teleportDuration = 3 }
        check(dsl.interpolationDuration == 2 && dsl.teleportDuration == 3) { "DSL duration properties should round-trip" }
    },

    Scenario("packets-static-ui-only-spawns") {
        standAndLook(STAND, ANCHOR)
        PacketRecorder.clear()
        onServer { JavaExamples.staticItemAndText(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 1, texts = 1)
        Thread.sleep(200)

        val spawn = PacketRecorder.snapshot()
        check(spawn.ofKind(Kind.SPAWN).size == 2) { "Expected 2 spawn packets, saw ${spawn.describe()}" }
        check(spawn.ofKind(Kind.METADATA).size == 2) { "Expected 2 spawn metadata packets, saw ${spawn.describe()}" }
        spawn.ofKind(Kind.METADATA).forEach {
            check(it.metadata(9) == 0) { "Spawn should not interpolate its transform, index 9 was ${it.metadata(9)}" }
            check(it.metadata(10) == 1) { "Spawn should set the default teleport duration, index 10 was ${it.metadata(10)}" }
        }

        val idle = PacketRecorder.during(2000)
        check(idle.isEmpty()) { "A static UI should send nothing once spawned, but sent ${idle.describe()} in 2 s" }
    },

    Scenario("packets-dynamic-position-at-rest-sends-nothing") {
        standAndLook(STAND, ANCHOR)
        // returns a new but equal location every tick, which used to look like movement
        onServer {
            JavaExamples.staticItemAndText(player, ANCHOR)
                .positionController(DynamicPositionController { MinestomLocation(ANCHOR) })
                .spawn()
        }
        awaitCounts(ANCHOR, items = 1, texts = 1)

        val idle = PacketRecorder.during(2000)
        check(idle.isEmpty()) { "An unmoving dynamic position should send nothing, but sent ${idle.describe()} in 2 s" }
    },

    Scenario("packets-only-the-animated-element-updates") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.oneSpinningChild(player, ANCHOR) }
        awaitCounts(ANCHOR, items = 3, texts = 0)

        val window = PacketRecorder.during(2000)
        val metadata = window.ofKind(Kind.METADATA)
        val updatedIds = metadata.flatMap { it.entityIds }.toSet()
        check(window.size == metadata.size) { "Only metadata should be sent, saw ${window.describe()}" }
        check(updatedIds.size == 1) { "Only the spinning child should update, but ${updatedIds.size} entities did" }
        check(metadata.size in 30..45) { "Expected ~40 updates in 2 s (one per tick), saw ${metadata.size}" }
        metadata.forEach {
            check(it.metadata(8) == 0 && it.metadata(9) == 1) {
                "Updates should start interpolating now over 1 tick, saw start ${it.metadata(8)} duration ${it.metadata(9)}"
            }
            check(!it.hasMetadata(10)) { "Offset updates should not resend the teleport duration" }
        }
    },

    Scenario("packets-custom-interpolation-is-sent") {
        standAndLook(STAND, ANCHOR)
        onServer { JavaExamples.pulsingScale(player, ANCHOR).interpolationDuration(3).spawn() }
        awaitCounts(ANCHOR, items = 1, texts = 0)

        val updates = PacketRecorder.during(500).ofKind(Kind.METADATA)
        check(updates.isNotEmpty()) { "Pulsing element should send updates" }
        updates.forEach {
            check(it.metadata(9) == 3) { "Updates should interpolate over 3 ticks, index 9 was ${it.metadata(9)}" }
        }
    },

    Scenario("packets-custom-teleport-duration-on-spawn") {
        standAndLook(STAND, ANCHOR)
        PacketRecorder.clear()
        onServer { JavaExamples.staticItemAndText(player, ANCHOR).teleportDuration(5).spawn() }
        awaitCounts(ANCHOR, items = 1, texts = 1)
        val spawnMetadata = PacketRecorder.snapshot().ofKind(Kind.METADATA)
        check(spawnMetadata.size == 2 && spawnMetadata.all { it.metadata(10) == 5 }) {
            "Spawn metadata should carry teleport duration 5, saw ${spawnMetadata.map { it.metadata(10) }}"
        }
    },

    Scenario("packets-moving-ui-sends-only-teleports") {
        standAndLook(STAND, ANCHOR)
        onServer { JavaExamples.slidingPosition(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 1, texts = 0, radius = 3.0)

        val window = PacketRecorder.during(1000)
        val teleports = window.ofKind(Kind.TELEPORT)
        check(window.size == teleports.size) { "A moving UI with a fixed offset should only teleport, saw ${window.describe()}" }
        check(teleports.size in 15..22) { "Expected ~20 teleports in 1 s, saw ${teleports.size}" }
    },

    Scenario("packets-riding-ui-never-teleports") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.ridingLabel(player) }
        Thread.sleep(500)

        PacketRecorder.clear()
        nebs.move(MoveDirection.LEFT, 20)
        nebs.look(90f, 0f)
        Thread.sleep(300)
        val window = PacketRecorder.snapshot()
        check(window.ofKind(Kind.TELEPORT).isEmpty()) {
            "Passengers follow their vehicle, so moving the player should not teleport them; saw ${window.describe()}"
        }
    },

    Scenario("packets-content-change-sends-one-update") {
        standAndLook(STAND, ANCHOR)
        lateinit var label: DSLRenderedElement
        onServer {
            diegetic {
                viewers = StaticViewerController(listOf(MinestomPlayer(player)))
                position = dsh.diegetic.position.StaticPositionController(MinestomLocation(ANCHOR))
                element {
                    draw {
                        text = Component.text("Before")
                        label = this
                    }
                }
            }
        }
        awaitCounts(ANCHOR, items = 0, texts = 1)

        PacketRecorder.clear()
        onServer { label.text = Component.text("After") }
        Thread.sleep(1000)
        val change = PacketRecorder.snapshot()
        check(change.size == 1 && change[0].kind == Kind.METADATA && change[0].metadata(23) == Component.text("After")) {
            "Changing the text should send exactly one text update, saw ${change.describe()}"
        }
        check(!change[0].hasMetadata(11)) { "A text change should not resend the transform" }

        PacketRecorder.clear()
        onServer { label.text = Component.text("After") }
        Thread.sleep(1000)
        val same = PacketRecorder.snapshot()
        check(same.isEmpty()) { "Setting an equal text should send nothing, saw ${same.describe()}" }
        Thread.sleep(200)
        screenshot("after")
    },

    Scenario("packets-unrendered-element-is-destroyed") {
        standAndLook(STAND, ANCHOR)
        val toggle = ToggleElement(element {
            draw { text = Component.text("Toggled") ; translation = org.joml.Vector3f(0f, 0.5f, 0f) }
        })
        onServer {
            diegetic {
                viewers = StaticViewerController(listOf(MinestomPlayer(player)))
                position = dsh.diegetic.position.StaticPositionController(MinestomLocation(ANCHOR))
                element {
                    draw { item = dsh.diegetic.interop.MinestomItem(net.minestom.server.item.ItemStack.of(net.minestom.server.item.Material.STONE)) }
                    child(toggle)
                }
            }
        }
        awaitCounts(ANCHOR, items = 1, texts = 1)

        PacketRecorder.clear()
        toggle.visible = false
        awaitCounts(ANCHOR, items = 1, texts = 0)
        Thread.sleep(500)
        val hide = PacketRecorder.snapshot()
        check(hide.size == 1 && hide[0].kind == Kind.DESTROY && hide[0].entityIds.size == 1) {
            "Hiding one element should send one destroy for it, saw ${hide.describe()}"
        }

        PacketRecorder.clear()
        toggle.visible = true
        awaitCounts(ANCHOR, items = 1, texts = 1)
        Thread.sleep(500)
        val show = PacketRecorder.snapshot()
        check(show.ofKind(Kind.SPAWN).size == 1 && show.size == 2) {
            "Showing it again should send one spawn and its metadata, saw ${show.describe()}"
        }
    },
)
