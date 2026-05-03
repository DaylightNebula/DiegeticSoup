package dsh.diegetic

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {
    override fun onEnable() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
        PacketEvents.getAPI().init()

        val api = PaperDiegeticAPI()
        api.init(this)
        DiegeticAPI.set(api)

        getCommand("test")?.setExecutor(TestCommand())
    }
}