package dsh.diegetic

import dsh.diegetic.interop.DPlayer
import dsh.diegetic.interop.MinestomPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule

class MinestomDiegeticAPI: DiegeticAPI {

    private val packetAPI = MinestomPacketAPI()
    private val controllers = mutableListOf<DiegeticController>()
    private var nextEntityID = Int.MAX_VALUE / 2

    fun init() {
        MinecraftServer.getSchedulerManager().scheduleTask({
            controllers.forEach(DiegeticController::tick)
            TaskSchedule.tick(1)
        }, TaskSchedule.tick(1))
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