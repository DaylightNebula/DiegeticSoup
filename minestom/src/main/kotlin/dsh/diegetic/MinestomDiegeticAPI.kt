package dsh.diegetic

import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.interop.MinestomPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule
import dsh.diegetic.interaction.ClickType
import dsh.diegetic.interaction.InteractionRegistry
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.network.packet.client.play.ClientAttackPacket
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket

class MinestomDiegeticAPI: DiegeticAPI {

    private val packetAPI = MinestomPacketAPI()
    private val controllers = mutableListOf<DiegeticController>()
    private var nextEntityID = Int.MAX_VALUE / 2

    fun init() {
        MinecraftServer.getSchedulerManager().scheduleTask({
            // iterate a copy: controllers (and click handlers they run) may add or remove controllers
            controllers.toList().forEach(DiegeticController::tick)
            TaskSchedule.tick(1)
        }, TaskSchedule.tick(1))

        // clicks on the invisible interaction entities of UIs
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent::class.java) { event ->
            val (entityId, type) = when (val packet = event.packet) {
                is ClientAttackPacket -> packet.targetId() to ClickType.LEFT
                is ClientInteractEntityPacket -> {
                    // the client sends one per hand; only the main hand counts as a click
                    if (packet.hand() != PlayerHand.MAIN) {
                        if (InteractionRegistry.isInteraction(packet.targetId())) event.isCancelled = true
                        return@addListener
                    }
                    packet.targetId() to ClickType.RIGHT
                }
                else -> return@addListener
            }
            if (InteractionRegistry.handle(MinestomPlayer(event.player), entityId, type)) event.isCancelled = true
        }
    }

    override fun nextEntityID() = nextEntityID++
    override fun getPacketAPI() = packetAPI
    override fun getActiveControllers() = controllers

    override fun getPlayersInWorld(world: String): Collection<DPlayer> {
        return MinecraftServer.getConnectionManager().onlinePlayers.map { MinestomPlayer(it) }
    }

    override fun addController(controller: DiegeticController) {
        controllers.add(controller)
    }

    override fun removeController(controller: DiegeticController) {
        controllers.remove(controller)
        controller.destroy()
    }
}