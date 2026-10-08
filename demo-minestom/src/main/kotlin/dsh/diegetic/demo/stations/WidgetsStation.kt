package dsh.diegetic.demo.stations

import dsh.diegetic.demo.Station
import dsh.diegetic.demo.mm
import dsh.diegetic.demo.show
import dsh.diegetic.flex.*
import dsh.diegetic.interaction.ClickType
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance

/** Clickable widgets: tabs, sliders, toggles, radio buttons and buttons, with hover highlights. */
object WidgetsStation : Station(
    "widgets",
    "Interactive widgets",
    "Tabs, sliders, toggles and buttons. Left and right click them!"
) {
    /** The settings panel, kept so the screenshot run can find its widgets. */
    lateinit var settings: FlexElement
    /** The shared click counter and its button. */
    lateinit var clickerPanelElement: FlexElement
    lateinit var clickerButton: FlexButton

    override fun build(origin: Pos, instance: Instance) {
        settings = settingsPanel()
        show(origin.add(-2.4, 2.1, 0.0), settings)
        clickerPanelElement = clickerPanel()
        show(origin.add(2.6, 2.1, 0.0), clickerPanelElement)
    }

    /** Settings with tabs: picking a tab swaps the widgets shown below it, which keep their state. */
    private fun settingsPanel(): FlexElement {
        val volumeLabel = FlexText.create(mm("<gray>Volume: 70"))
        val volume = FlexSlider.create(0f, 100f, 70f).step(5f).width(Length.percent(100f))
            .onChange { _, value -> volumeLabel.text(mm("<gray>Volume: ${value.toInt()}")) }
        val subtitles = FlexToggle.create(mm("<white>Subtitles")).checked(true)
        val quality = FlexRadioGroup.create<String>()
            .option("low", "Low").option("medium", "Medium").option("high", "High").selected("high")
        val vsync = FlexToggle.create(mm("<white>V-Sync"))
        val sensitivityLabel = FlexText.create(mm("<gray>Sensitivity: 50%"))
        val sensitivity = FlexSlider.create(0f, 100f, 50f).width(Length.percent(100f))
            .onChange { _, value -> sensitivityLabel.text(mm("<gray>Sensitivity: ${value.toInt()}%")) }
        val invert = FlexToggle.create(mm("<white>Invert mouse"))

        val content = FlexContainer.column().gap(5)
        fun showTab(tab: String) {
            content.clearChildren()
            when (tab) {
                "audio" -> content.children(volumeLabel, volume, subtitles)
                "video" -> content.children(FlexText.create(mm("<gray>Quality")), quality, vsync)
                else -> content.children(sensitivityLabel, sensitivity, invert)
            }
        }
        showTab("audio")

        return flex(scale = 0.55f) {
            direction(FlexDirection.COLUMN); width(190); padding(8); gap(8); background(0xE0101820)
            radioGroup("audio", RadioStyle.TABS, onChange = { _, tab -> showTab(tab) }) {
                option("audio", "Audio"); option("video", "Video"); option("controls", "Controls")
            }
            child(content)
            row {
                gap(6)
                button(onClick = { event -> event.player.let { println("${it.name()} saved their settings") } }) {
                    grow(1f); background(0xC0208040); hoverBackground(0xE030A050)
                    text(mm("<white>Save"))
                }
                button("Reset") { _ ->
                    volume.value(70f); volumeLabel.text(mm("<gray>Volume: 70")); subtitles.checked(true)
                }.grow(1f)
            }
        }
    }

    /** A counter shared by everyone: left click adds one, right click takes one away. */
    private fun clickerPanel(): FlexElement {
        var clicks = 0
        val count = FlexText.create(mm("<white><bold>0")).textAlign(dsh.diegetic.elements.TextAlignment.CENTER)
        val last = FlexText.create(mm("<gray>Nobody has clicked yet")).textAlign(dsh.diegetic.elements.TextAlignment.CENTER)

        return flex(scale = 0.55f) {
            direction(FlexDirection.COLUMN); width(150); padding(8); gap(6); background(0xE0101820)
            alignItems(AlignItems.STRETCH)
            text(mm("<gold><bold>Shared counter")) { textAlign(dsh.diegetic.elements.TextAlignment.CENTER) }
            child(count)
            clickerButton = button(onClick = { event ->
                clicks += if (event.type == ClickType.LEFT) 1 else -1
                count.text(mm("<white><bold>$clicks"))
                last.text(mm("<gray>${event.player.name()} ${if (event.isLeft) "added one" else "took one away"}"))
            }) {
                padding(6f, 8f); background(0xC0305080); hoverBackground(0xE04070B0)
                text(mm("<white>Left +1   Right -1"))
            }
            child(last)
        }
    }
}
