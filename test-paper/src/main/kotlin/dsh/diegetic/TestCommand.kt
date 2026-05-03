package dsh.diegetic

import dsh.diegetic.elements.DynamicParentElement
import dsh.diegetic.elements.StaticItemElement
import dsh.diegetic.elements.StaticParentElement
import dsh.diegetic.elements.StaticTextElement
import dsh.diegetic.interop.BukkitEntity
import dsh.diegetic.interop.BukkitItem
import dsh.diegetic.interop.BukkitLocation
import dsh.diegetic.interop.BukkitPlayer
import dsh.diegetic.position.PlayerPositionController
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.InRadiusViewerController
import dsh.diegetic.viewers.RemoveEmptyViewerController
import dsh.diegetic.viewers.SinglePlayerNearbyViewerController
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos

class TestCommand: CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        cmdStr: String,
        args: Array<String>
    ): Boolean {
        if (!(sender is Player)) return false
        if (!sender.isOp) return false

        if (args.isEmpty()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /test <test #>"))
            return true
        }

        when(args[0].toInt()) {
            0 -> test0(sender)
            1 -> test1(sender)
            2 -> test2(sender)
        }

        return true
    }

    fun test0(player: Player) {
        val location = player.location
        location.y += player.eyeHeight
        location.pitch = 0f

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = StaticViewerController(listOf(BukkitPlayer(player))),
                positionController = StaticPositionController(BukkitLocation(location)),
                element = StaticParentElement(
                    children = listOf(
                        StaticItemElement(
                            BukkitItem(ItemStack(Material.OAK_PLANKS)),
                            Matrix4f()
                        ),
                        StaticTextElement(
                            MiniMessage.miniMessage().deserialize("<red>Hello world!"),
                            Matrix4f().translate(0f, 1f, 0f)
                        )
                    )
                )
            )
        )
    }

    fun test1(player: Player) {
        val location = player.location
        location.y += player.eyeHeight
        location.pitch = 0f

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = RemoveEmptyViewerController(
                    child = InRadiusViewerController(BukkitLocation(location), 30.0)
                ),
                positionController = StaticPositionController(BukkitLocation(location)),
                element = DynamicParentElement(
                    children = listOf(
                        StaticItemElement(
                            BukkitItem(ItemStack(Material.OAK_PLANKS)),
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
        val location = player.location
        location.y += player.eyeHeight
        location.pitch = 0f

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(BukkitPlayer(player), BukkitLocation(location), 30.0),
                positionController = PlayerPositionController(BukkitPlayer(player), Vector3f()),
                parentEntity = BukkitEntity(player),
                element = StaticTextElement(
                    MiniMessage.miniMessage().deserialize("<red>Hello world!"),
                    Matrix4f().scale(0.5f).rotateY(PI.toFloat()).translate(0f, -0.2f, -1f)
                )
            )
        )
    }
}