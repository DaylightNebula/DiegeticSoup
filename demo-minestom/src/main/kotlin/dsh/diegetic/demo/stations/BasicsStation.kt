package dsh.diegetic.demo.stations

import dsh.diegetic.controller.*
import dsh.diegetic.demo.JavaBasics
import dsh.diegetic.demo.Station
import dsh.diegetic.demo.mm
import dsh.diegetic.demo.nearby
import dsh.diegetic.elements.*
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.position.StaticPositionController
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.sin

/** Item and text displays, animated with transforms: the same exhibit in the Kotlin DSL and in Java. */
object BasicsStation : Station(
    "basics",
    "The basics",
    "Item and text displays, sent as packets only, animated with transforms"
) {
    override fun build(origin: Pos, instance: Instance) {
        // two pedestals
        for (x in listOf(-2, 2)) {
            instance.setBlock(origin.blockX() + x, origin.blockY(), origin.blockZ(), Block.QUARTZ_PILLAR)
        }

        // the Kotlin DSL version, on the left
        val left = origin.add(-2.0, 1.9, 0.0)
        diegetic {
            viewers = nearby(left)
            position = StaticPositionController(MinestomLocation(left))

            element {
                val start = System.currentTimeMillis()
                child {
                    translate { Vector3f(0f, sin((System.currentTimeMillis() - start) / 600.0).toFloat() * 0.08f, 0f) }
                    rotation { Quaternionf().rotateY((System.currentTimeMillis() - start) / 900f) }
                    draw {
                        item = MinestomItem(ItemStack.of(Material.DIAMOND_BLOCK))
                        scale = Vector3f(0.6f)
                    }
                }
                child {
                    translate = Vector3f(0f, 0.75f, 0f)
                    draw {
                        text = mm("<aqua>Kotlin DSL")
                        scale = Vector3f(0.8f)
                    }
                }
            }
        }

        // the Java builder version, on the right
        JavaBasics.spinningEmerald(origin.add(2.0, 1.9, 0.0))

        // and a caption in between
        diegetic {
            viewers = nearby(origin)
            position = StaticPositionController(MinestomLocation(origin.add(0.0, 1.4, 0.5)))
            element {
                draw {
                    text = mm("<white>No real entities:\n<gray>each display exists only\n<gray>in players' clients")
                    scale = Vector3f(0.6f)
                }
            }
        }
    }
}
