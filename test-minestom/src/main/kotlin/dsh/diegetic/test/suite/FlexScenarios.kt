package dsh.diegetic.test.suite

import dsh.diegetic.flex.*
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.test.examples.FlexExamples
import dsh.diegetic.test.examples.KotlinExamples
import dsh.diegetic.test.suite.PacketRecorder.Kind
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.math.abs

private fun near(actual: Double, expected: Double, tolerance: Double) = abs(actual - expected) <= tolerance

val flexScenarios = listOf(

    // ---- text measurement, no client ----

    Scenario("flex-font-measures-text", needsClient = false) {
        fun width(text: String, bold: Boolean = false) =
            MinecraftFont.measure(Component.text(text).let { if (bold) it.decorate(TextDecoration.BOLD) else it }).width
        check(width("Hello") == 24f) { "Hello should be 24 px, was ${width("Hello")}" }
        check(width("ill") == 8f) { "ill should be 8 px, was ${width("ill")}" }
        check(width("WW", bold = true) == 14f) { "bold WW should be 14 px, was ${width("WW", true)}" }
        check(width("a b") == 16f) { "a b should be 16 px, was ${width("a b")}" }

        val nested = Component.text("ab").append(Component.text("cd").decorate(TextDecoration.BOLD))
        check(MinecraftFont.measure(nested).width == 26f) { "Bold children should add a pixel per glyph" }
    },

    Scenario("flex-font-wraps-like-the-client", needsClient = false) {
        fun lines(text: String, max: Float) = MinecraftFont.measure(Component.text(text), max)
        check(lines("WW WW", 13f) == MinecraftFont.TextMetrics(12f, 2)) { "WW WW at 13 px: ${lines("WW WW", 13f)}" }
        check(lines("Hello world", 30f) == MinecraftFont.TextMetrics(27f, 2)) { "Hello world at 30 px: ${lines("Hello world", 30f)}" }
        check(lines("Hello world", 55f).lines == 1 && lines("Hello world", 54f).lines == 2) { "Hello world is exactly 55 px" }
        check(lines("WWWWWW", 20f) == MinecraftFont.TextMetrics(18f, 2)) { "Long words break inside: ${lines("WWWWWW", 20f)}" }
        check(lines("a\nb", 200f).lines == 2) { "Explicit newlines break lines" }
        check(MinecraftFont.minContentWidth(Component.text("Hello world")) == 27f) { "min-content is the longest word" }
    },

    Scenario("flex-text-node-sizes", needsClient = false) {
        val paragraph = FlexText.create("The quick brown fox jumps over the lazy dog")
        val root = FlexContainer.column().width(81).child(paragraph)
        val box = FlexLayout.compute(root).getValue(paragraph)
        val expected = MinecraftFont.measure(paragraph.text, 80f)
        check(box.width == 81f && box.height == expected.height) { "Paragraph box should be 81 x ${expected.height}, was $box" }
        check(expected.lines == 4) { "Paragraph should wrap to 4 lines at 80 px (as the client does), wrapped to ${expected.lines}" }

        // text in a row shrinks to its longest word, wrapping the rest
        val label = FlexText.create("alpha beta gamma")
        val row = FlexContainer.row().width(40).child(label)
        val labelBox = FlexLayout.compute(row).getValue(label)
        check(labelBox.width == 40f && labelBox.height == 30f) { "Label should wrap to 3 lines in 40 px, was $labelBox" }
    },

    // ---- geometry against the real client ----

    Scenario("flex-text-matches-client") {
        listOf(
            "Hello world" to null,
            "The quick brown fox jumps over the lazy dog" to 81f,
            "Wrapping inside a narrow box" to 41f,
        ).forEachIndexed { i, (text, width) ->
            val node = FlexText.create(text).textBackground(MAGENTA)
            val root = FlexContainer.column().apply { width?.let { width(it) } }.child(node)
            val element = FlexElement.create(root).anchor(Anchor.TOP_LEFT)
            val box = element.layout().getValue(node)
            val m = measureMagenta("text-$i", element) ?: throw AssertionFailed("text $i was not drawn")
            // a text's background spans its measured width + 1, which is the box unless the box was stretched
            val metrics = MinecraftFont.measure(node.text, box.width - 1f)
            println("[suite]   '$text': layout ${metrics.width + 1} x ${metrics.height}, client $m")
            check(near(m.left, 0.0, 0.6) && near(m.top, 0.0, 0.6)) { "Text $i should start at the box corner, was $m" }
            check(near(m.width, metrics.width + 1.0, 0.8)) { "Text $i width: layout ${metrics.width + 1}, client ${m.width}" }
            check(near(m.height, metrics.height.toDouble(), 0.8)) {
                "Text $i wrapped differently: layout ${metrics.lines} lines, client ${m.height / 10} lines"
            }
        }
    },

    Scenario("flex-text-align-matches-client") {
        // "Hi" is 8 px (+1 background) inside a 60 px box
        listOf(dsh.diegetic.elements.TextAlignment.RIGHT to 51.0, dsh.diegetic.elements.TextAlignment.CENTER to 25.5).forEach { (align, left) ->
            val node = FlexText.create("Hi").textBackground(MAGENTA).textAlign(align)
            val element = FlexElement.create(FlexContainer.column().width(60).child(node)).anchor(Anchor.TOP_LEFT)
            val m = measureMagenta("align-${align.name.lowercase()}", element) ?: throw AssertionFailed("text was not drawn")
            println("[suite]   $align: layout x $left..${left + 9}, client $m")
            check(near(m.left, left, 0.6) && near(m.width, 9.0, 0.8)) { "$align text should span $left..${left + 9}, was $m" }
        }
    },

    Scenario("flex-background-matches-layout") {
        val inner = FlexContainer.create().size(30, 12).margin(4)
        val root = FlexContainer.row().size(60, 30).background(MAGENTA).child(inner)
        val m = measureMagenta("background", FlexElement.create(root)) ?: throw AssertionFailed("background was not drawn")
        println("[suite]   background: layout x -30..30 y -15..15, client $m")
        check(near(m.left, -30.0, 0.6) && near(m.right, 30.0, 0.6)) { "Background x should span -30..30, was $m" }
        check(near(m.bottom, -15.0, 0.6) && near(m.top, 15.0, 0.6)) { "Background y should span -15..15, was $m" }
    },

    Scenario("flex-item-matches-layout") {
        val item = FlexItem.create(MinestomItem(ItemStack.of(Material.MAGENTA_CONCRETE))).size(16, 16)
        val root = FlexContainer.row().size(48, 16).justifyContent(JustifyContent.FLEX_END).child(item)
        val m = measureMagenta("item", FlexElement.create(root)) ?: throw AssertionFailed("item was not drawn")
        // the cube's front face is nearer than the panel, so it looks up to ~5% larger
        println("[suite]   item: layout x 8..24 y -8..8, client $m")
        check(near((m.left + m.right) / 2, 16.0, 1.0) && near((m.bottom + m.top) / 2, 0.0, 1.0)) { "Item should be centred at (16, 0), was $m" }
        check(near(m.width, 16.0, 1.5) && near(m.height, 16.0, 1.5)) { "Item should be about 16 px, was $m" }
    },

    // ---- the example panels ----

    Scenario("flex-java-panel-renders") {
        standAndLook(STAND, ANCHOR)
        val element = FlexElement.create(FlexExamples.shopPanel())
        val layout = element.layout()
        val texts = layout.keys.count { it is FlexText }
        val items = layout.keys.count { it is FlexItem }
        val backgrounds = layout.keys.count { it is FlexContainer && it.background != null }
        onServer { FlexExamples.shop(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = items, texts = texts + backgrounds, radius = 6.0)
        Thread.sleep(500)
        screenshot("panel")
    },

    Scenario("flex-kotlin-panel-renders") {
        standAndLook(STAND, ANCHOR)
        onServer { KotlinExamples.flexShop(player, ANCHOR) }
        // 8 texts + 6 backgrounds (panel, 3 cards, 2 buttons), 3 items
        awaitCounts(ANCHOR, items = 3, texts = 14, radius = 6.0)
        Thread.sleep(500)
        screenshot("panel")
    },

    Scenario("flex-static-panel-sends-nothing") {
        standAndLook(STAND, ANCHOR)
        onServer { FlexExamples.shop(player, ANCHOR).spawn() }
        awaitCounts(ANCHOR, items = 3, texts = 14, radius = 6.0)
        val idle = PacketRecorder.during(2000)
        check(idle.isEmpty()) { "A static flex panel should send nothing after spawn, sent ${idle.describe()}" }
    },

    Scenario("flex-text-change-reflows") {
        standAndLook(STAND, ANCHOR)
        val label = FlexText.create("Short")
        val tag = FlexText.create("tag")
        val item = FlexItem.create(MinestomItem(ItemStack.of(Material.STONE)))
        val root = FlexContainer.column().width(160).padding(4).gap(4).background(0xC0202020)
            .child(FlexContainer.row().gap(4).children(label, tag))
            .child(FlexContainer.row().child(item))
        onServer {
            dsh.diegetic.controller.DSLController.create()
                .viewerController(dsh.diegetic.viewers.StaticViewerController(listOf(dsh.diegetic.interop.MinestomPlayer(player))))
                .positionController(dsh.diegetic.position.StaticPositionController(dsh.diegetic.interop.MinestomLocation(ANCHOR)))
                .element(FlexElement.create(root).scale(0.5f))
                .spawn()
        }
        awaitCounts(ANCHOR, items = 1, texts = 3)

        PacketRecorder.clear()
        onServer { label.text("A much longer label") }
        Thread.sleep(1000)
        val change = PacketRecorder.snapshot()
        val ids = change.flatMap { it.entityIds }.toSet()
        check(change.all { it.kind == Kind.METADATA }) { "A text change should only send metadata, sent ${change.describe()}" }
        check(label.entityId in ids && tag.entityId in ids) { "The label and the tag it pushes should update" }
        check(item.entityId !in ids && root.backgroundEntityId !in ids) { "The item and fixed-size panel should not update" }
        val perEntity = change.groupingBy { it.entityIds.single() }.eachCount()
        check(perEntity[tag.entityId] == 1) { "The tag should get one offset update, got ${perEntity[tag.entityId]}" }
        check(perEntity.getValue(label.entityId) <= 2) { "The label should get at most a text and an offset update" }
    },
)
