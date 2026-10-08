package dsh.diegetic.test.suite

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.DSLController
import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.elements.DSLElement
import dsh.diegetic.flex.*
import dsh.diegetic.interaction.ClickEvent
import dsh.diegetic.interaction.ClickType
import dsh.diegetic.interaction.HoverMode
import dsh.diegetic.interaction.InteractionRegistry
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.orientation
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.test.suite.PacketRecorder.Kind
import dsh.diegetic.viewers.StaticViewerController
import dsh.nebsclient.api.ClientOptions
import dsh.nebsclient.api.NebsClient
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.nio.file.Path
import java.util.Collections
import java.util.UUID
import kotlin.math.abs

/**
 * Panels for interaction tests sit a little above standing eye height, so a player facing them squarely
 * hovers just clear of the ground (y = 40) rather than a rounding error inside it.
 */
val PANEL_ORIGIN = Pos(0.5, 41.75, 4.5)

/** A player that only exists for client-free tests. */
class FakePlayer(private val location: DLocation, private val eye: Float = 0f) : DPlayer() {
    private val id = UUID.randomUUID()
    override fun uuid(): UUID = id
    override fun name() = "fake"
    override fun location() = location
    override fun eyeHeight() = eye
}

private fun Vector3f.near(other: Vector3f, tolerance: Float = 1e-4f) = distance(other) <= tolerance

/** A [ClickEvent] for calling widgets directly; the controller is a placeholder. */
private fun fakeClick(type: ClickType = ClickType.LEFT, x: Float? = null, width: Float = 0f, height: Float = 0f, drag: Boolean = false): ClickEvent {
    val controller = DiegeticController(
        StaticViewerController(emptyList()),
        StaticPositionController(MinestomLocation(ANCHOR)),
        FlexElement.create(FlexContainer.create())
    )
    return ClickEvent(FakePlayer(MinestomLocation(ANCHOR)), type, controller, x, 0f, width, height, drag)
}

/** The world position of a point inside [node]'s box, as fractions of its width and height (0.5, 0.5 = centre). */
fun nodeWorldPoint(element: FlexElement, node: FlexNode<*>, origin: Pos, fx: Float = 0.5f, fy: Float = 0.5f): Vector3f {
    val boxes = element.layout()
    val root = boxes.getValue(element.root)
    val box = boxes.getValue(node)
    val blocksPerPixel = element.scale / MinecraftFont.PIXELS_PER_BLOCK
    val local = Vector3f(
        (box.x + box.width * fx - element.anchor.x * root.width) * blocksPerPixel,
        (element.anchor.y * root.height - (box.y + box.height * fy)) * blocksPerPixel,
        0f
    )
    return local.rotate(MinestomLocation(origin).orientation()).add(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())
}

/** The direction the front of a panel at [origin] faces. */
fun panelNormal(origin: Pos): Vector3f = Vector3f(0f, 0f, 1f).rotate(MinestomLocation(origin).orientation())

/** Creative and flying, so the player can be placed anywhere and has the 5-block creative reach. */
fun TestContext.becomeInteractor(target: Player = player) = onServer {
    target.gameMode = GameMode.CREATIVE
    target.isAllowFlying = true
    target.isFlying = true
}

/**
 * Puts [target]'s eye [distance] blocks in front of [point] along [normal], then looks at the point.
 * Waits for the client to arrive before aiming (aiming from the old position looks the wrong way), and
 * for the server to receive the new view, since clicks and hover are resolved from the server's copy.
 */
fun TestContext.lookAtFrom(point: Vector3f, normal: Vector3f, distance: Float = 3f, target: Player = player, client: NebsClient = nebs) {
    val eye = Vector3f(normal).normalize().mul(distance).add(point)
    val feet = facing(Pos(eye.x.toDouble(), eye.y - target.eyeHeight, eye.z.toDouble()), eye, point)
    onServer {
        target.isFlying = true
        target.teleport(feet)
    }
    awaitCondition({ "The client should arrive at $feet, is at ${client.status().let { "(${it.x}, ${it.y}, ${it.z})" }}" }, timeoutMs = 3000) {
        val status = client.status()
        abs(status.x - feet.x) < 0.1 && abs(status.y - feet.y) < 0.1 && abs(status.z - feet.z) < 0.1
    }
    client.lookAt(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
    awaitCondition({
        val status = client.status()
        "The server should see the new view: client yaw ${status.yaw} pitch ${status.pitch}, server yaw ${target.position.yaw} pitch ${target.position.pitch}"
    }, timeoutMs = 3000) {
        val status = client.status()
        val pos = target.position
        angleDifference(pos.yaw, status.yaw.toFloat()) < 0.5f && abs(pos.pitch - status.pitch.toFloat()) < 0.5f
    }
    Thread.sleep(150)
}

fun TestContext.spawnPanel(element: FlexElement, origin: Pos = PANEL_ORIGIN, viewers: List<Player> = listOf(player), hoverMode: HoverMode = HoverMode.PER_VIEWER) =
    onServer {
        DSLController.create()
            .viewerController(StaticViewerController(viewers.map { MinestomPlayer(it) }))
            .positionController(StaticPositionController(MinestomLocation(origin)))
            .element(element)
            .hoverMode(hoverMode)
            .spawn()
    }

/** Waits for [condition], failing with [message] after [timeoutMs]. */
fun awaitCondition(message: String, timeoutMs: Long = 2000, condition: () -> Boolean) =
    awaitCondition({ message }, timeoutMs, condition)

/** Waits for [condition], failing with the message [describe] builds at that moment. */
fun awaitCondition(describe: () -> String, timeoutMs: Long = 2000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) throw AssertionFailed(describe())
        Thread.sleep(50)
    }
}

/**
 * [feet] facing from [eye] towards [point]. Teleporting with the final view keeps the server's copy in step:
 * if the client already faces that way, the following lookAt changes nothing and sends no rotation.
 */
fun facing(feet: Pos, eye: Vector3f, point: Vector3f): Pos {
    val d = Vector3f(point).sub(eye)
    val yaw = Math.toDegrees(kotlin.math.atan2(-d.x.toDouble(), d.z.toDouble())).toFloat()
    val pitch = Math.toDegrees(-kotlin.math.asin((d.y / d.length()).toDouble())).toFloat()
    return feet.withView(yaw, pitch)
}

/** Difference between two angles in degrees, ignoring whole turns (the client doesn't wrap its yaw). */
fun angleDifference(a: Float, b: Float): Float {
    val d = ((a - b) % 360f + 360f) % 360f
    return minOf(d, 360f - d)
}

/** Records the clicks a node receives, safely across threads. */
class ClickLog {
    val events: MutableList<Pair<String, ClickType>> = Collections.synchronizedList(mutableListOf())
    fun record(name: String) = java.util.function.Consumer<ClickEvent> { events += name to it.type }
    fun snapshot() = synchronized(events) { events.toList() }
}

val interactionScenarios = listOf(

    // ---- no client ----

    Scenario("interaction-orientation", needsClient = false) {
        fun facing(yaw: Float, pitch: Float) = Vector3f(0f, 0f, 1f).rotate(MinestomLocation(Pos(0.0, 0.0, 0.0, yaw, pitch)).orientation())
        check(facing(0f, 0f).near(Vector3f(0f, 0f, 1f))) { "yaw 0 should face +z, faced ${facing(0f, 0f)}" }
        check(facing(90f, 0f).near(Vector3f(-1f, 0f, 0f))) { "yaw 90 should face -x (west), faced ${facing(90f, 0f)}" }
        check(facing(180f, 0f).near(Vector3f(0f, 0f, -1f))) { "yaw 180 should face -z, faced ${facing(180f, 0f)}" }

        fun view(yaw: Float, pitch: Float) = Ray.view(FakePlayer(MinestomLocation(Pos(0.0, 0.0, 0.0, yaw, pitch))), MinestomLocation(Pos.ZERO)).direction
        check(view(0f, 0f).near(Vector3f(0f, 0f, 1f))) { "A player with yaw 0 looks along +z" }
        check(view(90f, 0f).near(Vector3f(-1f, 0f, 0f))) { "A player with yaw 90 looks along -x" }
        check(view(0f, 90f).near(Vector3f(0f, -1f, 0f))) { "A player with pitch 90 looks down" }
    },

    Scenario("interaction-hit-test", needsClient = false) {
        val buttons = List(3) { FlexButton.create("B$it").size(40, 20) }
        val root = FlexContainer.row().gap(10).padding(5).background(0xC0101010).children(*buttons.toTypedArray())

        for ((anchor, scale) in listOf(Anchor.CENTER to 1f, Anchor.TOP_LEFT to 0.5f, Anchor.BOTTOM_RIGHT to 2f)) {
            val element = FlexElement.create(root).anchor(anchor).scale(scale)
            buttons.forEachIndexed { i, button ->
                val center = nodeWorldPoint(element, button, Pos.ZERO)
                val hit = element.hitTest(Ray(Vector3f(center).add(0f, 0f, 2f), Vector3f(0f, 0f, -1f)), Matrix4f())
                check(hit?.target === button) { "$anchor x$scale: a ray at button $i hit ${hit?.target}" }
                check(abs(hit!!.x - 20f) < 0.01f && abs(hit.y - 10f) < 0.01f) { "Hit should be at the button centre, was (${hit.x}, ${hit.y})" }
                check(abs(hit.distance - 2f) < 0.01f) { "Hit distance should be 2, was ${hit.distance}" }
            }
            // the padding around the buttons isn't interactive, and the panel can't be hit from behind
            val padding = nodeWorldPoint(element, root, Pos.ZERO, 0.01f, 0.5f)
            check(element.hitTest(Ray(Vector3f(padding).add(0f, 0f, 2f), Vector3f(0f, 0f, -1f)), Matrix4f()) == null) { "Padding should not be hit" }
            val behind = nodeWorldPoint(element, buttons[0], Pos.ZERO)
            check(element.hitTest(Ray(Vector3f(behind).add(0f, 0f, -2f), Vector3f(0f, 0f, 1f)), Matrix4f()) == null) { "The back of the panel should not be hit" }
        }

        // transforms of parent elements apply: a panel turned to face +x is hit from the +x side
        val element = FlexElement.create(root)
        val turned = DSLElement.create().rotation(Quaternionf().rotateY(Math.toRadians(90.0).toFloat())).child(element)
        val center = nodeWorldPoint(element, buttons[2], Pos.ZERO).rotate(Quaternionf().rotateY(Math.toRadians(90.0).toFloat()))
        val hit = turned.hitTest(Ray(Vector3f(center).add(2f, 0f, 0f), Vector3f(-1f, 0f, 0f)), Matrix4f())
        check(hit?.target === buttons[2]) { "A rotated parent should still route hits to the right button, hit ${hit?.target}" }
    },

    Scenario("interaction-widget-state", needsClient = false) {
        // buttons report left and right clicks separately
        val log = ClickLog()
        val button = FlexButton.create("B").onLeftClick(log.record("left")).onRightClick(log.record("right")).onClick(log.record("any"))
        button.click(fakeClick(ClickType.LEFT))
        button.click(fakeClick(ClickType.RIGHT))
        check(log.snapshot() == listOf("left" to ClickType.LEFT, "any" to ClickType.LEFT, "right" to ClickType.RIGHT, "any" to ClickType.RIGHT)) {
            "Unexpected button events ${log.snapshot()}"
        }
        button.disabled(true).click(fakeClick())
        check(log.snapshot().size == 4) { "Disabled buttons ignore clicks" }

        // toggles flip and report the new state
        val changes = mutableListOf<Boolean>()
        val toggle = FlexToggle.create("T").onChange { _, checked -> changes += checked }
        toggle.click(fakeClick()); toggle.click(fakeClick(ClickType.RIGHT))
        check(changes == listOf(true, false) && !toggle.checked) { "Toggle should flip on each click, saw $changes" }
        check(toggle.indicator.children.isEmpty() && toggle.indicator.background == toggle.uncheckedColor) { "Unchecked toggle shows an empty box" }
        toggle.checked(true)
        check(toggle.indicator.children.size == 1 && toggle.indicator.background == toggle.checkedColor) { "Checked toggle shows a mark" }

        // radio groups keep one selection and only report changes
        val selections = mutableListOf<String>()
        val group = FlexRadioGroup.create<String>().option("a", "A").option("b", "B").option("c", "C")
            .selected("a").onChange { _, value -> selections += value }
        group.options[1].click(fakeClick()); group.options[1].click(fakeClick()); group.options[2].click(fakeClick())
        check(selections == listOf("b", "c") && group.selected == "c") { "Radio selections were $selections" }
        check(group.options.count { it.isSelected } == 1) { "Exactly one option should be selected" }
        group.style(RadioStyle.TABS)
        check(group.options.none { group.options.first().dot in it.children } && group.options[2].background == group.tabSelectedBackground) {
            "Tabs drop the dots and highlight the selected tab"
        }

        // sliders map the click position along the track, accounting for the thumb, and snap to steps
        val values = mutableListOf<Float>()
        val slider = FlexSlider.create(0f, 100f, 0f).onChange { _, value -> values += value }
        // 100 px wide, 8 px tall: the thumb centre travels from x 4 to x 96
        slider.click(fakeClick(x = 4f + 0.25f * 92f, width = 100f, height = 8f))
        check(abs(slider.value - 25f) < 0.01f) { "Clicking a quarter along should give 25, gave ${slider.value}" }
        slider.step(10f).drag(fakeClick(ClickType.RIGHT, x = 4f + 0.57f * 92f, width = 100f, height = 8f, drag = true))
        check(slider.value == 60f) { "Steps of 10 should snap 57 to 60, gave ${slider.value}" }
        slider.click(fakeClick(x = 200f, width = 100f, height = 8f))
        check(slider.value == 100f) { "Clicks past the end clamp to the maximum, gave ${slider.value}" }
        check(values == listOf(25f, 60f, 100f)) { "onChange should report each new value, reported $values" }
    },

    // ---- in world ----

    Scenario("interaction-button-left-right") {
        becomeInteractor()
        val log = ClickLog()
        val left = FlexButton.create("Left").size(50, 24).onClick(log.record("left"))
        val right = FlexButton.create("Right").size(50, 24).onClick(log.record("right"))
        val element = FlexElement.create(FlexContainer.row().gap(30).children(left, right))

        PacketRecorder.clear()
        spawnPanel(element)
        awaitCondition("Both buttons should spawn an interaction entity") {
            val spawned = PacketRecorder.snapshot().ofKind(Kind.SPAWN).flatMap { it.entityIds }
            left.interactionEntityId in spawned && right.interactionEntityId in spawned
        }

        lookAtFrom(nodeWorldPoint(element, left, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
        nebs.attack()
        awaitCondition("Left-clicking the left button should reach it") { log.snapshot().size == 1 }
        nebs.use()
        awaitCondition("Right-clicking the left button should reach it") { log.snapshot().size == 2 }

        lookAtFrom(nodeWorldPoint(element, right, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
        nebs.attack()
        awaitCondition("Left-clicking the right button should reach it") { log.snapshot().size == 3 }
        Thread.sleep(300)
        check(log.snapshot() == listOf("left" to ClickType.LEFT, "left" to ClickType.RIGHT, "right" to ClickType.LEFT)) {
            "Clicks went to ${log.snapshot()}"
        }
    },

    Scenario("interaction-rotated-panel") {
        becomeInteractor()
        onServer { player.isInvisible = true }
        for ((yaw, pitch) in listOf(90f to 0f, 200f to -30f)) {
            val log = ClickLog()
            // laid out diagonally, so every button is off both axes and both yaw and pitch move it
            val aligns = listOf(AlignSelf.FLEX_START, AlignSelf.CENTER, AlignSelf.FLEX_END)
            val buttons = List(3) { i ->
                FlexButton.create("$i").size(30, 20).alignSelf(aligns[i]).onClick(log.record("$i")).hoverBackground(null)
            }
            val element = FlexElement.create(FlexContainer.column().width(120).gap(10).children(*buttons.toTypedArray()))
            val origin = PANEL_ORIGIN.withView(yaw, pitch)
            val controller = spawnPanel(element, origin)
            Thread.sleep(300)

            listOf(2, 0).forEach { index ->
                // paint the target magenta: if our idea of the panel's rotation differed from the client's, the
                // centre of the view would not land on it (or would see the panel's back, which doesn't render)
                buttons.forEachIndexed { i, button -> button.background(if (i == index) MAGENTA else 0xFF202020.toInt()) }
                lookAtFrom(nodeWorldPoint(element, buttons[index], origin), panelNormal(origin))
                val shot = screenshot("yaw${yaw.toInt()}-pitch${pitch.toInt()}-button$index")
                // sample around the centre rather than one pixel, which may land on the button's label
                val image = javax.imageio.ImageIO.read(shot.toFile())
                val samples = (-4..4).flatMap { dx -> (-4..4).map { dy -> image.getRGB(image.width / 2 + dx * 8, image.height / 2 + dy * 4) } }
                val magenta = samples.count { isMagenta(it shr 16 and 0xff, it shr 8 and 0xff, it and 0xff) }
                check(magenta > samples.size / 2) {
                    "yaw $yaw pitch $pitch: the client doesn't show button $index where the server thinks it is " +
                        "(only $magenta of ${samples.size} pixels around the centre are magenta)"
                }

                nebs.attack()
                awaitCondition("yaw $yaw pitch $pitch: button $index should be clicked, clicks ${log.snapshot()}") {
                    log.snapshot().lastOrNull()?.first == "$index"
                }
            }
            check(log.snapshot().map { it.first } == listOf("2", "0")) { "yaw $yaw pitch $pitch: clicks went to ${log.snapshot()}" }
            onServer { DiegeticAPI.get().removeController(controller) }
        }
        onServer { player.isInvisible = false }
    },

    Scenario("interaction-toggle") {
        becomeInteractor()
        val changes = Collections.synchronizedList(mutableListOf<Boolean>())
        val toggle = FlexToggle.create("Enabled").onChange { _, checked -> changes += checked }
        val element = FlexElement.create(FlexContainer.column().padding(6).background(0xC0101820).child(toggle))
        spawnPanel(element)
        lookAtFrom(nodeWorldPoint(element, toggle, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))

        PacketRecorder.clear()
        nebs.attack()
        awaitCondition("The toggle should switch on") { changes.toList() == listOf(true) }
        Thread.sleep(300)
        val records = PacketRecorder.snapshot()
        check(records.any { it.kind == Kind.METADATA && toggle.indicator.backgroundEntityId in it.entityIds }) {
            "The indicator's colour should be updated, saw ${records.describe()}"
        }
        screenshot("checked")

        nebs.use()
        awaitCondition("The toggle should switch off again") { changes.toList() == listOf(true, false) }
    },

    Scenario("interaction-radio-and-tabs") {
        becomeInteractor()
        val dotChanges = Collections.synchronizedList(mutableListOf<String>())
        val tabChanges = Collections.synchronizedList(mutableListOf<String>())
        val dots = FlexRadioGroup.create<String>().option("easy", "Easy").option("hard", "Hard")
            .selected("easy").onChange { _, value -> dotChanges += value }
        val tabs = FlexRadioGroup.create<String>().style(RadioStyle.TABS)
            .option("stats", "Stats").option("items", "Items").option("help", "Help")
            .selected("stats").onChange { _, value -> tabChanges += value }
        val element = FlexElement.create(FlexContainer.column().gap(8).padding(6).background(0xC0101820).children(tabs, dots))
        spawnPanel(element)
        Thread.sleep(300)

        lookAtFrom(nodeWorldPoint(element, dots.options[1], PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
        nebs.attack()
        awaitCondition("Picking Hard should select it") { dots.selected == "hard" }
        nebs.attack()
        Thread.sleep(300)
        check(dotChanges.toList() == listOf("hard")) { "Re-picking the selected option should not report a change, saw $dotChanges" }

        lookAtFrom(nodeWorldPoint(element, tabs.options[2], PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
        nebs.attack()
        awaitCondition("Picking the Help tab should select it") { tabs.selected == "help" }
        check(tabChanges.toList() == listOf("help")) { "Tabs reported $tabChanges" }
        check(tabs.options.count { it.isSelected } == 1) { "Exactly one tab should be selected" }
        lookAtFrom(nodeWorldPoint(element, element.root, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN), distance = 4f)
        Thread.sleep(300)
        screenshot("selected")
    },

    Scenario("interaction-slider-click-and-drag") {
        becomeInteractor()
        val values = Collections.synchronizedList(mutableListOf<Float>())
        val slider = FlexSlider.create(0f, 100f, 50f).size(120, 10).onChange { _, value -> values += value }
        val element = FlexElement.create(FlexContainer.column().padding(6).background(0xC0101820).child(slider))
        spawnPanel(element)
        Thread.sleep(300)

        // the thumb centre travels from 5 px to 115 px, so 25% of the travel is at 5 + 0.25 * 110 = 32.5 px of 120
        val normal = panelNormal(PANEL_ORIGIN)
        lookAtFrom(nodeWorldPoint(element, slider, PANEL_ORIGIN, 32.5f / 120f), normal)
        nebs.attack()
        awaitCondition("Clicking a quarter along the track should set about 25") { values.isNotEmpty() }
        check(abs(slider.value - 25f) < 3f) { "Expected about 25, got ${slider.value}" }

        // drag: hold right click on one connection to the client while another moves the view along the track
        val home = Path.of(System.getProperty("nebs.home", ".nebs")).toAbsolutePath()
        val looker = NebsClient.attach(nebs.name, home)
        val holder = Thread { runCatching { nebs.use(40) } }
        values.clear()
        holder.start()
        for (fraction in listOf(0.35f, 0.5f, 0.65f, 0.8f)) {
            Thread.sleep(350)
            val point = nodeWorldPoint(element, slider, PANEL_ORIGIN, (5f + fraction * 110f) / 120f)
            runCatching { looker.lookAt(point.x.toDouble(), point.y.toDouble(), point.z.toDouble()) }
        }
        holder.join(10_000)
        val dragged = values.toList()
        println("[suite]   drag values: $dragged")
        check(dragged.size >= 2) { "Holding right click while moving along the track should update the value repeatedly, saw $dragged" }
        check(dragged.zipWithNext().all { (a, b) -> b >= a }) { "Dragging right should only increase the value, saw $dragged" }
        check(slider.value > 60f) { "The drag should end near 80, ended at ${slider.value}" }
    },

    Scenario("interaction-disabled-button") {
        becomeInteractor()
        val log = ClickLog()
        val button = FlexButton.create("Off").size(60, 24).disabled(true).onClick(log.record("off"))
        val element = FlexElement.create(FlexContainer.row().child(button))
        PacketRecorder.clear()
        spawnPanel(element)
        awaitDisplays(Pos(PANEL_ORIGIN.x, PANEL_ORIGIN.y, PANEL_ORIGIN.z), "the disabled button") { it.size >= 2 }
        lookAtFrom(nodeWorldPoint(element, button, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
        runCatching { nebs.attack() }
        Thread.sleep(500)
        check(log.snapshot().isEmpty()) { "A disabled button should not be clickable" }
        val spawned = PacketRecorder.snapshot().ofKind(Kind.SPAWN).flatMap { it.entityIds }
        check(button.interactionEntityId !in spawned) { "A disabled button should not spawn an interaction entity" }
    },

    Scenario("interaction-hover-modes") {
        becomeInteractor()
        val home = Path.of(System.getProperty("nebs.home", ".nebs")).toAbsolutePath()
        val second = nebs.spawn(ClientOptions().name("Watcher").home(home).readyTimeout(java.time.Duration.ofMinutes(5)))
        try {
            second.connect("localhost", 25565)
            second.setHudVisible(false)
            val watcher = MinecraftServer.getConnectionManager().onlinePlayers.first { it.username == "Watcher" }
            becomeInteractor(watcher)

            for (mode in listOf(HoverMode.PER_VIEWER, HoverMode.SHARED)) {
                val button = FlexButton.create("Hover me").size(80, 24)
                val element = FlexElement.create(FlexContainer.row().child(button))
                val controller = spawnPanel(element, viewers = listOf(player, watcher), hoverMode = mode)
                try {
                Thread.sleep(500)

                // both players start looking away, so the hover shows up as an update rather than in the spawn
                onServer {
                    watcher.teleport(Pos(4.5, 42.0, 8.5, 0f, -60f))
                    player.teleport(Pos(0.5, 42.0, 9.5, 0f, -60f))
                }
                Thread.sleep(300)
                second.look(0f, -60f)
                awaitCondition("$mode: nobody should hover the button yet") { controller.getHovered(MinestomPlayer(player)) == null }
                Thread.sleep(200)
                PacketRecorder.clear()
                lookAtFrom(nodeWorldPoint(element, button, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN))
                awaitCondition("$mode: the button should be hovered") { controller.getHovered(MinestomPlayer(player)) === button }
                Thread.sleep(300)

                val recipients = PacketRecorder.snapshot()
                    .filter { it.kind == Kind.METADATA && button.backgroundEntityId in it.entityIds }
                    .map { it.player.username }.toSet()
                println("[suite]   $mode: hover update sent to $recipients")
                val expected = if (mode == HoverMode.PER_VIEWER) setOf(player.username) else setOf(player.username, "Watcher")
                check(recipients == expected) { "$mode: hover highlight should go to $expected, went to $recipients" }
                } finally {
                    onServer { DiegeticAPI.get().removeController(controller) }
                    Thread.sleep(300)
                }
            }
        } finally {
            runCatching { second.close() }
        }
    },

    Scenario("interaction-example-panels") {
        becomeInteractor()
        for ((name, spawn) in listOf<Pair<String, () -> DiegeticController>>(
            "java" to { dsh.diegetic.test.examples.FlexExamples.settings(player, PANEL_ORIGIN).spawn() },
            "kotlin" to { dsh.diegetic.test.examples.KotlinExamples.flexSettings(player, PANEL_ORIGIN) },
        )) {
            val controller = onServer { spawn() }
            val element = controller.element as FlexElement
            lookAtFrom(nodeWorldPoint(element, element.root, PANEL_ORIGIN), panelNormal(PANEL_ORIGIN), distance = 3.5f)
            Thread.sleep(400)
            screenshot(name)

            // drag-free click on the slider: clicking near the left end sets a low value and relabels it
            val slider = element.layout().keys.filterIsInstance<FlexSlider>().single()
            lookAtFrom(nodeWorldPoint(element, slider, PANEL_ORIGIN, 0.1f), panelNormal(PANEL_ORIGIN), distance = 3.5f)
            nebs.attack()
            awaitCondition("$name: clicking the slider should change the volume") { slider.value < 20f }
            val label = element.layout().keys.filterIsInstance<FlexText>().first { "Volume" in net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it.text) }
            awaitCondition("$name: the volume label should follow the slider") {
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(label.text) == "Volume: ${slider.value.toInt()}"
            }
            onServer { DiegeticAPI.get().removeController(controller) }
            Thread.sleep(300)
        }
    },

    Scenario("interaction-reach") {
        val log = ClickLog()
        val button = FlexButton.create("Reach").size(60, 24).onClick(log.record("button"))
        val element = FlexElement.create(FlexContainer.row().child(button))
        val point = nodeWorldPoint(element, button, PANEL_ORIGIN)
        val reach = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)

        /** Stands on the ground [distance] blocks in front of the button, looking at it, in [mode]. */
        fun standAt(distance: Double, mode: GameMode) {
            onServer {
                player.gameMode = mode
                player.isFlying = false
                player.isAllowFlying = false
            }
            val feet = facing(Pos(point.x.toDouble(), 40.0, point.z + distance),
                Vector3f(point.x, 40f + player.eyeHeight.toFloat(), (point.z + distance).toFloat()), point)
            onServer { player.teleport(feet) }
            awaitCondition("The client should arrive at $feet", timeoutMs = 3000) {
                val status = nebs.status()
                abs(status.x - feet.x) < 0.1 && abs(status.z - feet.z) < 0.1 && status.onGround
            }
            nebs.lookAt(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            awaitCondition("The server should see the new view", timeoutMs = 3000) {
                val status = nebs.status()
                angleDifference(player.position.yaw, status.yaw.toFloat()) < 0.5f && abs(player.position.pitch - status.pitch.toFloat()) < 0.5f
            }
            Thread.sleep(200)
        }

        /** Whether the button ends up hovered, and whether a real click from the client reaches it. */
        fun hoverAndClick(controller: DiegeticController): Pair<Boolean, Boolean> {
            val hovered = runCatching {
                awaitCondition("hover", timeoutMs = 600) { controller.getHovered(MinestomPlayer(player)) === button }
            }.isSuccess
            val before = log.snapshot().size
            runCatching { nebs.attack() }
            val clicked = runCatching { awaitCondition("click", timeoutMs = 600) { log.snapshot().size > before } }.isSuccess
            return hovered to clicked
        }

        val controller = spawnPanel(element)
        try {
            standAt(4.0, GameMode.SURVIVAL)
            check(hoverAndClick(controller) == (false to false)) { "Survival reach is 3: at 4 blocks there should be no hover or click" }

            // a modified client could send the click anyway: the server must refuse it
            InteractionRegistry.handle(MinestomPlayer(player), button.interactionEntityId, ClickType.LEFT)
            Thread.sleep(300)
            check(log.snapshot().isEmpty()) { "A click from beyond reach must be rejected" }

            standAt(2.5, GameMode.SURVIVAL)
            check(hoverAndClick(controller) == (true to true)) { "At 2.5 blocks a survival player should hover and click" }

            // Minestom doesn't add vanilla's creative reach modifier, so creative reach is the attribute's 3 blocks
            // too, on the client as well as the server: hover and clicks must still agree
            standAt(4.0, GameMode.CREATIVE)
            check(hoverAndClick(controller) == (false to false)) { "Creative reach on Minestom is 3: at 4 blocks there should be no hover or click" }

            onServer { reach.baseValue = 6.0 }
            standAt(5.5, GameMode.SURVIVAL)
            check(hoverAndClick(controller) == (true to true)) { "With a reach attribute of 6, 5.5 blocks should hover and click" }
        } finally {
            onServer {
                reach.baseValue = Attribute.ENTITY_INTERACTION_RANGE.defaultValue()
                DiegeticAPI.get().removeController(controller)
            }
        }

        // the controller's cap applies on top of the player's reach
        val capped = onServer {
            DSLController.create()
                .viewerController(StaticViewerController(listOf(MinestomPlayer(player))))
                .positionController(StaticPositionController(MinestomLocation(PANEL_ORIGIN)))
                .element(element)
                .interactionRange(2f)
                .spawn()
        }
        try {
            standAt(2.5, GameMode.CREATIVE)
            check(!hoverAndClick(capped).first) { "A controller capped at 2 blocks should not hover at 2.5" }
            val before = log.snapshot().size
            InteractionRegistry.handle(MinestomPlayer(player), button.interactionEntityId, ClickType.LEFT)
            Thread.sleep(300)
            // the client may click (its reach is 5), but the server applies the cap
            check(log.snapshot().size == before) { "A capped controller should reject clicks beyond its cap" }
        } finally {
            onServer { DiegeticAPI.get().removeController(capped) }
        }
    },

    Scenario("interaction-entities-cleanup") {
        becomeInteractor()
        val keep = FlexButton.create("Keep").size(50, 20)
        val drop = FlexButton.create("Drop").size(50, 20)
        val row = FlexContainer.row().gap(10).children(keep, drop)
        val controller = spawnPanel(FlexElement.create(row))
        awaitCondition("Both buttons should be registered") {
            InteractionRegistry.isInteraction(keep.interactionEntityId) && InteractionRegistry.isInteraction(drop.interactionEntityId)
        }

        PacketRecorder.clear()
        onServer { row.removeChild(drop) }
        awaitCondition("Removing a button should unregister its interaction entity") { !InteractionRegistry.isInteraction(drop.interactionEntityId) }
        Thread.sleep(200)
        check(PacketRecorder.snapshot().ofKind(Kind.DESTROY).any { drop.interactionEntityId in it.entityIds }) {
            "The removed button's interaction entity should be destroyed"
        }

        PacketRecorder.clear()
        onServer { DiegeticAPI.get().removeController(controller) }
        Thread.sleep(300)
        check(!InteractionRegistry.isInteraction(keep.interactionEntityId)) { "Removing the controller should unregister everything" }
        check(PacketRecorder.snapshot().ofKind(Kind.DESTROY).any { keep.interactionEntityId in it.entityIds }) {
            "Removing the controller should destroy its interaction entities"
        }
    },
)

fun Pos.withView(yaw: Float, pitch: Float) = Pos(x, y, z, yaw, pitch)
