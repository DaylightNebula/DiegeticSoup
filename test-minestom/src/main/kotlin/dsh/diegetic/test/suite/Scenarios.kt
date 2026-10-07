package dsh.diegetic.test.suite

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.*
import dsh.diegetic.elements.*
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.test.examples.JavaExamples
import dsh.diegetic.test.examples.KotlinExamples
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.function.Supplier

class Scenario(val name: String, val needsClient: Boolean = true, val body: TestContext.() -> Unit)

/** Where displays are anchored, around eye level (ground surface is y = 40). */
val ANCHOR = Pos(0.5, 41.2, 4.5)
/**
 * Where the player stands for every in-world scenario: south of the anchor, facing north,
 * because unrotated text displays only render from their front (+z) side.
 */
val STAND = Pos(0.5, 40.0, 8.5, 180f, 0f)

/** Allowed difference between two screenshots of a scene that should not change. */
const val STATIC_DIFF_LIMIT = 0.005
/** Minimum difference between two screenshots of a scene that should be animating. */
const val ANIMATED_DIFF_MIN = 0.01

val scenarios = listOf(

    // ---- Builder behaviour that needs no client ----

    Scenario("builder-rejects-missing-fields", needsClient = false) {
        val viewers = StaticViewerController(emptyList())
        val position = StaticPositionController(MinestomLocation(ANCHOR))

        val noViewer = expectThrows<IllegalStateException>("build() without a viewer controller") {
            DSLController.create().positionController(position).element(DSLElement.create()).build()
        }
        check(noViewer.message == "ViewerController not initialized") { "Unexpected message: ${noViewer.message}" }

        val noPosition = expectThrows<IllegalStateException>("build() without a position controller") {
            DSLController.create().viewerController(viewers).element(DSLElement.create()).build()
        }
        check(noPosition.message == "PositionController not initialized") { "Unexpected message: ${noPosition.message}" }

        val noElement = expectThrows<IllegalStateException>("build() without an element") {
            DSLController.create().viewerController(viewers).positionController(position).build()
        }
        check(noElement.message == "Element not initialized") { "Unexpected message: ${noElement.message}" }
    },

    Scenario("builder-methods-are-fluent", needsClient = false) {
        val controller = DSLController.create()
        check(controller.viewerController(StaticViewerController(emptyList())) === controller) { "viewerController() should return this" }
        check(controller.parentEntity(null) === controller) { "parentEntity() should return this" }

        val element = DSLElement.create()
        check(element.translate(Vector3f()) === element) { "translate() should return this" }
        check(element.scale(Supplier { Vector3f(1f) }) === element) { "scale(Supplier) should return this" }
        check(element.child(DSLElement.create()) === element) { "child() should return this" }

        val rendered = DSLRenderedElement.create()
        check(rendered.text(Component.text("hi")).scale(Vector3f(2f)) === rendered) { "DSLRenderedElement setters should return this" }
    },

    Scenario("kotlin-dsl-properties-round-trip", needsClient = false) {
        val viewers = StaticViewerController(emptyList())
        val position = StaticPositionController(MinestomLocation(ANCHOR))
        val controller = DSLController.create().apply {
            this.viewers = viewers
            this.position = position
        }
        check(controller.viewerController === viewers) { "viewers= should set viewerController" }
        check(controller.positionController === position) { "position= should set positionController" }

        val element = element {
            translate = Vector3f(1f, 2f, 3f)
            scale = Vector3f(2f)
        }
        check(element.translate == Vector3f(1f, 2f, 3f)) { "translate getter returned ${element.translate}" }
        val expected = Matrix4f().translate(1f, 2f, 3f).scale(2f)
        check(element.transform.equals(expected, 1e-5f)) { "transform was ${element.transform}" }

        val rendered = DSLRenderedElement.create().apply {
            item = MinestomItem(ItemStack.of(Material.STONE))
            rotation = Quaternionf().rotateY(1f)
        }
        check(rendered.item != null && rendered.text == null) { "item= should set an item" }
        rendered.text = Component.text("now text")
        check(rendered.item == null && rendered.text != null) { "text= should replace the item" }
        check(rendered.rotation == Quaternionf().rotateY(1f)) { "rotation getter returned ${rendered.rotation}" }
    },

    Scenario("element-suppliers-are-evaluated-each-render", needsClient = false) {
        var calls = 0
        val element = DSLElement.create().scale(Supplier { calls++; Vector3f(calls.toFloat()) })
        val first = element.transform.getScale(Vector3f())
        val second = element.transform.getScale(Vector3f())
        check(calls == 2) { "Supplier should be called once per transform, was called $calls times" }
        check(first.x == 1f && second.x == 2f) { "Scale should follow the supplier: $first then $second" }
    },

    // ---- In-world scenarios driven by NebsClient ----

    Scenario("java-build-does-not-spawn") {
        standAndLook(STAND, ANCHOR)
        val built = onServer { JavaExamples.staticItemAndText(player, ANCHOR).build() }
        Thread.sleep(1000)
        check(built !in DiegeticAPI.get().getActiveControllers()) { "build() must not register the controller" }
        check(displaysNear(ANCHOR).isEmpty()) { "build() must not show anything, saw ${displaysNear(ANCHOR).summary()}" }
    },

    Scenario("java-static-item-and-text") {
        standAndLook(STAND, ANCHOR)
        val controller = onServer { JavaExamples.staticItemAndText(player, ANCHOR).spawn() }
        check(controller in DiegeticAPI.get().getActiveControllers()) { "spawn() must register the controller" }

        val displays = awaitCounts(ANCHOR, items = 1, texts = 1)
        check(displays.all { it.distanceTo(ANCHOR) < 0.1 }) { "Displays should sit at the anchor: ${displays.summary()}" }

        Thread.sleep(500)
        val a = screenshot("a")
        Thread.sleep(600)
        val b = screenshot("b")
        val diff = centralPixelDifference(a, b)
        println("[suite]   screenshot diff %.4f".format(diff))
        check(diff < STATIC_DIFF_LIMIT) { "Static display changed between screenshots (diff %.4f)".format(diff) }
    },

    Scenario("kotlin-static-item-and-text") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.staticItemAndText(player, ANCHOR) }

        val displays = awaitCounts(ANCHOR, items = 1, texts = 1)
        check(displays.all { it.distanceTo(ANCHOR) < 0.1 }) { "Displays should sit at the anchor: ${displays.summary()}" }
        Thread.sleep(500)
        screenshot("a")
    },

    Scenario("kotlin-autospawn-false-does-not-spawn") {
        standAndLook(STAND, ANCHOR)
        val built = onServer { KotlinExamples.staticItemAndText(player, ANCHOR, autoSpawn = false) }
        Thread.sleep(1000)
        check(built !in DiegeticAPI.get().getActiveControllers()) { "autoSpawn = false must not register the controller" }
        check(displaysNear(ANCHOR).isEmpty()) { "autoSpawn = false must not show anything" }

        onServer { DiegeticAPI.get().addController(built) }
        awaitCounts(ANCHOR, items = 1, texts = 1)
    },

    Scenario("java-nested-children") {
        standAndLook(STAND, ANCHOR)
        onServer { JavaExamples.nestedChildren(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 3, texts = 1)
        Thread.sleep(500)
        screenshot("a")
    },

    Scenario("kotlin-nested-children") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.nestedChildren(player, ANCHOR) }
        awaitCounts(ANCHOR, items = 3, texts = 1)
        Thread.sleep(500)
        screenshot("a")
    },

    Scenario("java-pulsing-scale-animates") {
        standAndLook(STAND, ANCHOR)
        onServer { JavaExamples.pulsingScale(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 1, texts = 0)
        Thread.sleep(500)

        val diff = maxAnimatedDifference()
        check(diff > ANIMATED_DIFF_MIN) { "Pulsing scale did not change the picture (diff %.4f)".format(diff) }
    },

    Scenario("kotlin-wobbling-translate-animates") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.wobblingTranslate(player, ANCHOR) }
        awaitCounts(ANCHOR, items = 1, texts = 0)
        Thread.sleep(500)

        val diff = maxAnimatedDifference()
        check(diff > ANIMATED_DIFF_MIN) { "Wobbling translate did not change the picture (diff %.4f)".format(diff) }
    },

    Scenario("java-dynamic-position-moves-entity") {
        standAndLook(STAND, ANCHOR)
        onServer { JavaExamples.slidingPosition(player, ANCHOR).spawn() }

        val xs = mutableListOf<Double>()
        val deadline = System.currentTimeMillis() + 2500
        while (System.currentTimeMillis() < deadline) {
            displaysNear(ANCHOR, radius = 3.0).firstOrNull()?.let { xs += it.x }
            Thread.sleep(150)
        }
        check(xs.size >= 5) { "Sliding display was rarely visible (${xs.size} samples)" }
        val range = xs.max() - xs.min()
        check(range > 1.0) { "Display root should slide by ~3 blocks, but x only ranged %.2f".format(range) }
    },

    Scenario("kotlin-parent-entity-rides-player") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.ridingLabel(player) }
        val playerPos = Pos(STAND.x, STAND.y + 1.0, STAND.z)
        awaitDisplays(playerPos, "a text display riding the player", radius = 3.0) {
            it.count { e -> e.type.endsWith("text_display") } == 1
        }
    },

    Scenario("remove-controller-despawns") {
        standAndLook(STAND, ANCHOR)
        val controller = onServer { JavaExamples.nestedChildren(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 3, texts = 1)

        onServer { DiegeticAPI.get().removeController(controller) }
        awaitDisplays(ANCHOR, "every display to be removed") { it.isEmpty() }
        check(controller !in DiegeticAPI.get().getActiveControllers()) { "Controller should be unregistered" }
    },
)
