package dsh.diegetic.test.utils

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.controller.diegetic
import dsh.diegetic.controller.element
import dsh.diegetic.controller.position
import dsh.diegetic.controller.positionController
import dsh.diegetic.controller.viewerController
import dsh.diegetic.controller.viewers
import dsh.diegetic.elements.draw
import dsh.diegetic.elements.item
import dsh.diegetic.elements.rotation
import dsh.diegetic.elements.scale
import dsh.diegetic.elements.text
import dsh.diegetic.elements.translation
import dsh.diegetic.elements.DSLElement
import dsh.diegetic.elements.DynamicParentElement
import dsh.diegetic.elements.StaticItemElement
import dsh.diegetic.elements.StaticTextElement
import dsh.diegetic.elements.element
import dsh.diegetic.interop.DItem
import dsh.diegetic.interop.MinestomEntity
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.PlayerPositionController
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.InRadiusViewerController
import dsh.diegetic.viewers.RemoveEmptyViewerController
import dsh.diegetic.viewers.SinglePlayerNearbyViewerController
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class TestCommand: Command("test") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Usage: /test <#>")
        }

        var numberArgument = ArgumentType.Integer("number")

        addSyntax({ sender, context ->
            val number = context.get(numberArgument)
            if (number == null) return@addSyntax
            when (number) {
                0 -> test0(sender as Player)
                1 -> test1(sender as Player)
                2 -> test2(sender as Player)
                3 -> test3(sender as Player)
                4 -> test4(sender as Player)
                else -> sender.sendMessage("Invalid test number!")
            }
        }, numberArgument)
    }

    fun test0(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        diegetic {
            viewers = StaticViewerController(listOf(MinestomPlayer(player)))
            position = StaticPositionController(MinestomLocation(location))

            element {
                val startTime = System.currentTimeMillis()

                scale {
                    val time = System.currentTimeMillis()
                    val diff = (time - startTime) % 2500 / 2500f
                    val scale = sin(diff * 2f * 3.1415f)
                    Vector3f(scale * 0.25f + 0.5f)
                }

                draw {
                    item = MinestomItem(ItemStack.of(Material.OAK_PLANKS))
                }

                draw {
                    text = MiniMessage.miniMessage().deserialize("<red>Hello world!")
                    translation = Vector3f(0f, 1f, 0f)
                }

                draw {
                    text = MiniMessage.miniMessage().deserialize("<red>Hello world!")
                    translation = Vector3f(0f, 1f, 0f)
                    rotation = Quaternionf().rotateY(Math.PI.toFloat())
                }
            }
        }
    }

    fun test1(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = RemoveEmptyViewerController(
                    child = InRadiusViewerController(MinestomLocation(location), 30.0)
                ),
                positionController = StaticPositionController(MinestomLocation(location)),
                element = DynamicParentElement(
                    children = listOf(
                        StaticItemElement(
                            MinestomItem(ItemStack.of(Material.OAK_PLANKS)),
                            Matrix4f()
                        )
                    ),
                    initialize = { System.currentTimeMillis() },
                    update = { startTime, _, _ ->
                        val timeSinceStart = System.currentTimeMillis() - startTime
                        val pulse = cos((timeSinceStart % 3000) / 1500.0 * PI) * 0.5f + 0.5f
                        val scale = pulse.toFloat() * 0.5f + 0.5f
                        Matrix4f().scale(scale, scale, scale)
                    }
                )
            )
        )
    }

    fun test2(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(location), 30.0),
                positionController = PlayerPositionController(MinestomPlayer(player), Vector3f()),
                parentEntity = MinestomEntity(player),
                element = StaticTextElement(
                    MiniMessage.miniMessage().deserialize("<red>Hello world!"),
                    Matrix4f().scale(0.5f).rotateY(PI.toFloat()).translate(0f, -0.2f, -1f)
                )
            )
        )
    }

    fun test3(player: Player) {
        player.instance.setBlock(player.position.blockX(), player.position.blockY(), player.position.blockZ(), Block.YELLOW_CONCRETE)

        val location = Pos(player.position.blockX().toDouble(), player.position.blockY().toDouble(), player.position.blockZ().toDouble())
            .withX { it + 0.5 }
            .withY { it + 1.025 }
            .withZ { it + 1.0 }
            .withPitch { -90.0 }
            .withYaw { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(location), 30.0),
                positionController = StaticPositionController(MinestomLocation(location)),
                element = StaticTextElement(
                    text = MiniMessage.miniMessage().deserialize("W"),
                    offset = Matrix4f().scale(2.15f)
                )
            )
        )
    }

    fun test4(player: Player) {
        val direction = player.position.direction()
        val location = player.position.add(0.0, player.eyeHeight, 0.0).add(direction)

        diegetic {
            viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(location), 12.0)
            positionController = StaticPositionController(MinestomLocation(location))

            element {
                child(rotatingElement(
                    item = MinestomItem(ItemStack.of(Material.GRAVEL)),
                    targetAngle = 135f,
                    itemScale = 0.3f
                ))
                child(rotatingElement(
                    item = MinestomItem(ItemStack.of(Material.DIRT)),
                    targetAngle = 90f,
                    itemScale = 0.3f
                ))
                child(rotatingElement(
                    item = MinestomItem(ItemStack.of(Material.APPLE)),
                    targetAngle = 45f,
                    itemScale = 0.3f
                ))
                child(rotatingElement(
                    item = MinestomItem(ItemStack.of(Material.FLINT)),
                    targetAngle = 0f,
                    itemScale = 0.3f
                ))
            }
        }
    }
}

fun rotatingElement(
    item: DItem,
    targetAngle: Float,
    itemScale: Float = 0.5f,
    circleScale: Float = 0.5f,
    fanTime: Float = 500f
): DSLElement {
    return element {

        val startTime = System.currentTimeMillis()

        translate {
            val diff = min((System.currentTimeMillis() - startTime) / fanTime, 1f)
            val angle = diff * targetAngle
            Vector3f(sin(angle * -0.01745277777).toFloat() * circleScale, cos(angle * 0.01745277777).toFloat() * circleScale, 0f)
        }

        draw {
            this.item = item
            scale = Vector3f(itemScale)
        }

        draw {
            text = MiniMessage.miniMessage().deserialize("<red>Hello world!")
            rotation = Quaternionf().rotateY(Math.PI.toFloat())
            translation = Vector3f(0f, itemScale / 2f, 0f)
            scale = Vector3f(itemScale * 1.5f)
        }
    }
}
