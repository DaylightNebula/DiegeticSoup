package dsh.diegetic.controller

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.interop.DEntity
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController
import org.joml.Matrix4f
import java.util.*

/**
 * Shows an element to a set of viewers and keeps it up to date.
 *
 * Every tick the element is rendered and compared with what was last sent; packets are only sent for
 * entities whose offset, content or position actually changed, or to viewers that just started watching.
 *
 * @param interpolationDuration Ticks the client takes to smoothly blend an entity to a new offset.
 *                              1 suits elements animated every tick; 0 snaps.
 * @param teleportDuration      Ticks the client takes to smoothly move entities to a new position (0-59).
 */
class DiegeticController(
    val viewerController: ViewerController,
    val positionController: PositionController,
    val element: DiegeticElement,
    val parentEntity: DEntity? = null,
    val interpolationDuration: Int = DEFAULT_INTERPOLATION_DURATION,
    val teleportDuration: Int = DEFAULT_TELEPORT_DURATION
) {
    companion object {
        const val DEFAULT_INTERPOLATION_DURATION = 1
        const val DEFAULT_TELEPORT_DURATION = 1
        const val MAX_TELEPORT_DURATION = 59
    }

    init {
        require(interpolationDuration >= 0) { "interpolationDuration must not be negative" }
        require(teleportDuration in 0..MAX_TELEPORT_DURATION) { "teleportDuration must be between 0 and $MAX_TELEPORT_DURATION" }
    }

    private var rootPosition = positionController.getPosition()
    private val viewers = mutableSetOf<DPlayer>()

    // the state last sent to viewers for each entity
    private val entities = mutableMapOf<Int, RenderedElement>()

    fun getViewers(): Collection<DPlayer> = viewers
    fun getRootPosition(): DLocation = rootPosition

    fun tick() {
        val position = positionController.getPosition()

        // update viewers set
        val (toAdd, toRemove, removeController) = viewerController.tick(viewers, position)

        if (removeController) {
            DiegeticAPI.get().removeController(this)
            return
        }

        // update root position; passengers follow their vehicle, so they never need moving
        val moved = !position.isSameAs(rootPosition)
        if (moved) rootPosition = position
        val sendMoves = moved && parentEntity == null

        // render element
        val rendered = LinkedList<RenderedElement>()
        element.render(rendered, Matrix4f())

        val packets = DiegeticAPI.get().getPacketAPI()
        val stale = entities.keys.toMutableSet()
        var activeEntitiesChanged = false

        rendered.forEach { element ->
            stale.remove(element.entityId)
            val previous = entities.put(element.entityId, element)

            when {
                // new entity: spawn it for current and new viewers
                previous == null -> {
                    spawn(viewers + toAdd, element)
                    activeEntitiesChanged = true
                }

                // switched between item and text: the entity type changed, so recreate it
                previous::class != element::class -> {
                    if (viewers.isNotEmpty()) packets.removeEntities(viewers, listOf(element.entityId))
                    spawn(viewers + toAdd, element)
                    activeEntitiesChanged = true
                }

                else -> {
                    if (viewers.isNotEmpty()) updateExisting(previous, element, sendMoves)
                    if (toAdd.isNotEmpty()) {
                        spawn(toAdd, element)
                        activeEntitiesChanged = true
                    }
                }
            }
        }

        // remove entities that were not rendered this tick
        if (stale.isNotEmpty()) {
            if (viewers.isNotEmpty()) packets.removeEntities(viewers, stale)
            stale.forEach(entities::remove)
            activeEntitiesChanged = true
        }

        if (parentEntity != null && activeEntitiesChanged && (viewers.isNotEmpty() || toAdd.isNotEmpty())) {
            packets.setEntityPassengers(viewers + toAdd, parentEntity.id(), entities.keys)
        }

        // save viewer updates
        viewers.addAll(toAdd)
        viewers.removeAll(toRemove.toSet())

        // remove entities for viewers that have been removed
        if (toRemove.isNotEmpty() && entities.isNotEmpty()) {
            packets.removeEntities(toRemove, entities.keys)
        }
    }

    fun destroy() {
        if (viewers.isEmpty() || entities.isEmpty()) return
        DiegeticAPI.get()
            .getPacketAPI()
            .removeEntities(viewers, entities.keys)
    }

    /** Sends only what changed between [previous] and [current] to the existing viewers. */
    private fun updateExisting(previous: RenderedElement, current: RenderedElement, moved: Boolean) {
        val packets = DiegeticAPI.get().getPacketAPI()

        if (previous.offset != current.offset) {
            packets.updateDisplayOffset(viewers, current.entityId, current.offset, interpolationDuration)
        }

        when {
            current is RenderedElement.Item && previous is RenderedElement.Item && current.item != previous.item ->
                packets.updateItemDisplay(viewers, current.entityId, current.item)
            current is RenderedElement.Text && previous is RenderedElement.Text &&
                (current.text != previous.text || current.options != previous.options) ->
                packets.updateTextDisplay(viewers, current.entityId, current.text, current.options)
        }

        if (moved) packets.moveEntity(viewers, current.entityId, rootPosition)
    }

    private fun spawn(targets: Collection<DPlayer>, element: RenderedElement) {
        if (targets.isEmpty()) return
        val packets = DiegeticAPI.get().getPacketAPI()
        when (element) {
            is RenderedElement.Item ->
                packets.spawnItemDisplay(targets, element.entityId, element.item, rootPosition, element.offset, teleportDuration)
            is RenderedElement.Text ->
                packets.spawnTextDisplay(targets, element.entityId, element.text, rootPosition, element.offset, teleportDuration, element.options)
        }
    }
}

/** Compares locations by value, since [DLocation] implementations need not implement equals. */
private fun DLocation.isSameAs(other: DLocation) =
    this === other || (world() == other.world() &&
        position() == other.position() &&
        yaw() == other.yaw() &&
        pitch() == other.pitch())
