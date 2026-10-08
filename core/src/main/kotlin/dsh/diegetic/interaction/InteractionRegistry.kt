package dsh.diegetic.interaction

import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.interop.DPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps the interaction entities of every UI to their controller, so platforms can route clicks.
 * Platforms call [handle] from any thread; the click is processed on the controller's next tick.
 */
object InteractionRegistry {
    private val owners = ConcurrentHashMap<Int, DiegeticController>()

    internal fun register(entityId: Int, controller: DiegeticController) { owners[entityId] = controller }
    internal fun unregister(entityId: Int) { owners.remove(entityId) }

    /** True when [entityId] is an interaction entity of some UI. */
    @JvmStatic
    fun isInteraction(entityId: Int): Boolean = owners.containsKey(entityId)

    /**
     * Queues a click by [player] on interaction entity [entityId].
     * Returns false when the entity isn't part of any UI, so the platform should handle the packet normally.
     */
    @JvmStatic
    fun handle(player: DPlayer, entityId: Int, type: ClickType): Boolean {
        val controller = owners[entityId] ?: return false
        controller.queueClick(player, entityId, type)
        return true
    }
}
