package dsh.diegetic

import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.interop.DEntity
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController
import org.joml.Matrix4f
import java.util.*

class DiegeticController(
    val viewerController: ViewerController,
    val positionController: PositionController,
    val element: DiegeticElement,
    val parentEntity: DEntity? = null
) {
    private var rootPosition = positionController.getPosition()
    private val viewers = mutableSetOf<DPlayer>()
    private val entityIds = mutableMapOf<Int, Matrix4f>()

    fun getViewers(): Collection<DPlayer> = viewers
    fun getRootPosition(): DLocation = rootPosition

    fun tick() {
        // update viewers set
        val (toAdd, toRemove, removeController) = viewerController.tick(viewers, positionController.getPosition())

        if (removeController) {
            DiegeticAPI.get().removeController(this)
            return
        }

        // update root position
        val newPosition = positionController.getPosition()
        val moved = newPosition != rootPosition
        if (moved) rootPosition = newPosition

        // render element
        val elements = LinkedList<RenderedElement>()
        element.render(elements, Matrix4f())

        // add and remove entity IDs, update position during iteration for kept entities
        val removed = entityIds.toMutableMap()
        var activeEntitiesChanged = false
        elements.forEach { element ->
            val lastOffset = removed.remove(element.entityId)

            if (lastOffset != null) {
                if (lastOffset != element.offset) {
                    DiegeticAPI.get()
                        .getPacketAPI()
                        .updateDisplayOffset(viewers, element.entityId, element.offset)
                    entityIds[element.entityId] = element.offset
                }

                // spawn for all to add players
                when(element) {
                    is RenderedElement.Item -> DiegeticAPI.get()
                        .getPacketAPI()
                        .spawnItemDisplay(toAdd, element.entityId, element.item, rootPosition, element.offset)
                    is RenderedElement.Text -> DiegeticAPI.get()
                        .getPacketAPI()
                        .spawnTextDisplay(toAdd, element.entityId, element.text, rootPosition, element.offset)
                }
                if (toAdd.isNotEmpty()) activeEntitiesChanged = true

                if (moved) {
                    DiegeticAPI.get()
                        .getPacketAPI()
                        .moveEntity(viewers, element.entityId, rootPosition )
                }
            } else {
                entityIds[element.entityId] = element.offset

                // spawn entity for all current and future viewers
                when(element) {
                    is RenderedElement.Item -> DiegeticAPI.get()
                        .getPacketAPI()
                        .spawnItemDisplay(viewers + toAdd, element.entityId, element.item, rootPosition, element.offset)
                    is RenderedElement.Text -> DiegeticAPI.get()
                        .getPacketAPI()
                        .spawnTextDisplay(viewers + toAdd, element.entityId, element.text, rootPosition, element.offset)
                }

                activeEntitiesChanged = true
            }
        }

        if (parentEntity != null && activeEntitiesChanged) {
            DiegeticAPI.get()
                .getPacketAPI()
                .setEntityPassengers(viewers + toAdd, parentEntity.id(), entityIds.keys)
        }

        // save viewer updates
        viewers.addAll(toAdd)
        viewers.removeAll(toRemove.toSet())

        // remove entities to viewers that have been removed
        DiegeticAPI.get()
            .getPacketAPI()
            .removeEntities(toRemove, entityIds.keys)
    }

    fun destroy() {
        DiegeticAPI.get()
            .getPacketAPI()
            .removeEntities(viewers, entityIds.keys)
    }
}
