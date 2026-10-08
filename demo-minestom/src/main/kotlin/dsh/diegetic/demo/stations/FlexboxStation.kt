package dsh.diegetic.demo.stations

import dsh.diegetic.demo.JavaShopPanel
import dsh.diegetic.demo.Station
import dsh.diegetic.demo.mm
import dsh.diegetic.demo.show
import dsh.diegetic.flex.*
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance

/** Flexbox layouts: a shop panel built in Java, and a panel comparing justify-content options. */
object FlexboxStation : Station(
    "flexbox",
    "Flexbox layouts",
    "Containers, text and items laid out like CSS flexbox"
) {
    private val colors = listOf(0xFFE06C75.toInt(), 0xFF98C379.toInt(), 0xFF61AFEF.toInt())

    override fun build(origin: Pos, instance: Instance) {
        show(origin.add(-2.6, 2.0, 0.0), FlexElement.create(JavaShopPanel.create()).scale(0.55f))
        show(origin.add(2.6, 2.0, 0.0), justifyPanel())
    }

    /** One row per justify-content value, each with the same three boxes. */
    private fun justifyPanel() = flex(scale = 0.55f) {
        direction(FlexDirection.COLUMN); width(210); padding(8); gap(5); background(0xF0101820)

        text(mm("<gold><bold>justify-content"))
        listOf(
            JustifyContent.FLEX_START, JustifyContent.CENTER, JustifyContent.FLEX_END,
            JustifyContent.SPACE_BETWEEN, JustifyContent.SPACE_EVENLY
        ).forEach { justify ->
            text(mm("<gray>${justify.name.lowercase().replace('_', '-')}"))
            row {
                justifyContent(justify); padding(3); background(0xFF2A3340)
                colors.forEach { color -> container { size(16, 10); background(color) } }
            }
        }
        text(mm("<gold><bold>align-items"))
        row {
            gap(6); height(30)
            listOf(AlignItems.FLEX_START, AlignItems.CENTER, AlignItems.FLEX_END, AlignItems.STRETCH).forEach { align ->
                row {
                    grow(1f); alignItems(align); justifyContent(JustifyContent.CENTER); background(0xFF2A3340)
                    container { width(10); if (align != AlignItems.STRETCH) height(10); background(colors[1]) }
                }
            }
        }
    }
}
