package dsh.diegetic.test.examples

import dsh.diegetic.controller.*
import dsh.diegetic.elements.*
import dsh.diegetic.interop.MinestomEntity
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.PlayerPositionController
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.sin

/**
 * Examples of building diegetic UIs with the Kotlin DSL.
 */
object KotlinExamples {

    /** An oak plank with a label floating above it. */
    fun staticItemAndText(viewer: Player, at: Pos, autoSpawn: Boolean = true) = diegetic(autoSpawn) {
        viewers = StaticViewerController(listOf(MinestomPlayer(viewer)))
        position = StaticPositionController(MinestomLocation(at))

        element {
            draw {
                item = MinestomItem(ItemStack.of(Material.OAK_PLANKS))
                scale = Vector3f(0.5f)
            }

            draw {
                text = MiniMessage.miniMessage().deserialize("<aqua>Kotlin DSL")
                translation = Vector3f(0f, 0.5f, 0f)
            }
        }
    }

    /** Three items laid out in a row by child elements, with a shared label. */
    fun nestedChildren(viewer: Player, at: Pos) = diegetic {
        viewers = StaticViewerController(listOf(MinestomPlayer(viewer)))
        position = StaticPositionController(MinestomLocation(at))

        element {
            draw {
                text = MiniMessage.miniMessage().deserialize("<gold>Kotlin children")
                translation = Vector3f(0f, 0.5f, 0f)
            }

            listOf(Material.STONE, Material.DIRT, Material.GOLD_BLOCK).forEachIndexed { index, material ->
                child {
                    translate = Vector3f((index - 1) * 0.6f, 0f, 0f)
                    draw {
                        item = MinestomItem(ItemStack.of(material))
                        scale = Vector3f(0.4f)
                    }
                }
            }
        }
    }

    /** An item that wobbles side to side through an animated element translation. */
    fun wobblingTranslate(viewer: Player, at: Pos) = diegetic {
        viewers = StaticViewerController(listOf(MinestomPlayer(viewer)))
        position = StaticPositionController(MinestomLocation(at))

        element {
            val startTime = System.currentTimeMillis()

            translate {
                val t = (System.currentTimeMillis() - startTime) / 1000.0
                Vector3f((sin(t * 2 * PI) * 0.6).toFloat(), 0f, 0f)
            }

            draw {
                item = MinestomItem(ItemStack.of(Material.REDSTONE_BLOCK))
                scale = Vector3f(0.5f)
            }
        }
    }

    /**
     * Three items where only the middle one spins. Only that one entity is updated each tick, and
     * interpolating over one tick lets the client draw the spin smoothly between updates.
     */
    fun oneSpinningChild(viewer: Player, at: Pos) = diegetic {
        viewers = StaticViewerController(listOf(MinestomPlayer(viewer)))
        position = StaticPositionController(MinestomLocation(at))
        interpolationDuration = 1

        element {
            listOf(Material.STONE, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK).forEachIndexed { index, material ->
                child {
                    translate = Vector3f((index - 1) * 0.6f, 0f, 0f)
                    if (index == 1) {
                        val startTime = System.currentTimeMillis()
                        rotation { Quaternionf().rotateY(((System.currentTimeMillis() - startTime) / 1000.0 * PI).toFloat()) }
                    }
                    draw {
                        item = MinestomItem(ItemStack.of(material))
                        scale = Vector3f(0.4f)
                    }
                }
            }
        }
    }

    /** A label that rides on the viewer, using the parent entity. */
    fun ridingLabel(viewer: Player) = diegetic {
        viewers = StaticViewerController(listOf(MinestomPlayer(viewer)))
        position = PlayerPositionController(MinestomPlayer(viewer), Vector3f())
        parentEntity = MinestomEntity(viewer)

        element {
            draw {
                text = MiniMessage.miniMessage().deserialize("<red>Riding")
                translation = Vector3f(0f, 0.5f, 0f)
            }
        }
    }
}
