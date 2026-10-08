package dsh.diegetic.demo.stations

import dsh.diegetic.controller.*
import dsh.diegetic.demo.Station
import dsh.diegetic.demo.mm
import dsh.diegetic.demo.nearby
import dsh.diegetic.elements.*
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.position.StaticPositionController
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Animation: an orbit of items, and the same stepped motion with and without client interpolation. */
object AnimationStation : Station(
    "animation",
    "Animation",
    "Transforms that change every tick, smoothed by client-side interpolation"
) {
    private val orbitItems = listOf(
        Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.REDSTONE,
        Material.LAPIS_LAZULI, Material.AMETHYST_SHARD, Material.COPPER_INGOT, Material.QUARTZ
    )

    override fun build(origin: Pos, instance: Instance) {
        val center = origin.add(-2.2, 2.0, 0.0)
        diegetic {
            viewers = nearby(center)
            position = StaticPositionController(MinestomLocation(center))
            element {
                val start = System.currentTimeMillis()
                draw {
                    item = MinestomItem(ItemStack.of(Material.NETHER_STAR))
                    scale = Vector3f(0.7f)
                }
                orbitItems.forEachIndexed { index, material ->
                    child {
                        translate {
                            val angle = (System.currentTimeMillis() - start) / 1200.0 + index * 2 * PI / orbitItems.size
                            // a ring tilted towards the viewer, so it reads as a circle rather than a line
                            Vector3f((cos(angle) * 1.2).toFloat(), (sin(angle) * 0.8).toFloat(), (sin(angle) * 0.5).toFloat())
                        }
                        rotation { Quaternionf().rotateY((System.currentTimeMillis() - start) / 400f) }
                        draw {
                            item = MinestomItem(ItemStack.of(material))
                            scale = Vector3f(0.55f)
                        }
                    }
                }
            }
        }

        // the same motion, updated only every quarter second: snapping, and interpolated over those 5 ticks
        steppedCube(origin.add(1.3, 1.4, 0.0), Material.RED_CONCRETE, interpolation = 0, label = "<red>interpolation 0")
        steppedCube(origin.add(3.7, 1.4, 0.0), Material.LIME_CONCRETE, interpolation = 5, label = "<green>interpolation 5")
    }

    private fun steppedCube(at: Pos, material: Material, interpolation: Int, label: String) = diegetic {
        viewers = nearby(at)
        position = StaticPositionController(MinestomLocation(at))
        interpolationDuration = interpolation

        element {
            child {
                translate {
                    val step = System.currentTimeMillis() / 250
                    Vector3f(0f, (sin(step * 0.6) * 0.6 + 0.6).toFloat(), 0f)
                }
                draw {
                    item = MinestomItem(ItemStack.of(material))
                    scale = Vector3f(0.45f)
                }
            }
            draw {
                text = mm(label)
                translation = Vector3f(0f, -0.5f, 0f)
                scale = Vector3f(0.8f)
            }
        }
    }
}
