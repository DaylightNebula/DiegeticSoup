package dsh.diegetic.controller

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.interaction.ClickEvent
import dsh.diegetic.interaction.ClickType
import dsh.diegetic.interaction.Hit
import dsh.diegetic.interaction.HoverMode
import dsh.diegetic.interaction.Interactive
import dsh.diegetic.interaction.InteractionRegistry
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interaction.orientation
import dsh.diegetic.interop.DEntity
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Shows an element to a set of viewers and keeps it up to date.
 *
 * Every tick the element is rendered and compared with what each viewer last received; packets are
 * only sent for entities whose offset, content or position actually changed, or to viewers that just
 * started watching. Interactive elements get invisible interaction entities so players can click them;
 * clicks and hovers are resolved by casting the player's view against the UI.
 *
 * @param interpolationDuration Ticks the client takes to smoothly blend an entity to a new offset.
 *                              1 suits elements animated every tick; 0 snaps.
 * @param teleportDuration      Ticks the client takes to smoothly move entities to a new position (0-59).
 * @param hoverMode             Whether hover highlights are per viewer or shared by everyone watching.
 * @param interactionRange      An optional cap, in blocks, on how far away viewers can hover and click the
 *                              UI. Each player's own reach ([DPlayer.interactionRange]) always applies.
 */
class DiegeticController(
    val viewerController: ViewerController,
    val positionController: PositionController,
    val element: DiegeticElement,
    val parentEntity: DEntity? = null,
    val interpolationDuration: Int = DEFAULT_INTERPOLATION_DURATION,
    val teleportDuration: Int = DEFAULT_TELEPORT_DURATION,
    val hoverMode: HoverMode = HoverMode.PER_VIEWER,
    val interactionRange: Float = DEFAULT_INTERACTION_RANGE
) {
    companion object {
        const val DEFAULT_INTERPOLATION_DURATION = 1
        const val DEFAULT_TELEPORT_DURATION = 1
        const val MAX_TELEPORT_DURATION = 59
        /** No cap beyond each player's own reach. */
        const val DEFAULT_INTERACTION_RANGE = Float.POSITIVE_INFINITY
        /**
         * Extra distance allowed for clicks beyond the reach used for hover, so a click that was in reach
         * on the client isn't rejected because the server's copy of the player lags a tick behind.
         */
        const val CLICK_REACH_TOLERANCE = 0.5f
        /** Ticks between held right-click repeats that still count as one drag (the client repeats every 4). */
        const val DRAG_WINDOW_TICKS = 6
        private const val MIN_HITBOX_SIZE = 0.1f
    }

    init {
        require(interpolationDuration >= 0) { "interpolationDuration must not be negative" }
        require(teleportDuration in 0..MAX_TELEPORT_DURATION) { "teleportDuration must be between 0 and $MAX_TELEPORT_DURATION" }
        require(interactionRange > 0f) { "interactionRange must be positive" }
    }

    /** Where an interaction entity's hitbox sits in the world. */
    private data class Placement(val position: Vector3f, val width: Float, val height: Float)

    /** What a viewer last received for an entity. */
    private class Sent(val element: RenderedElement, val placement: Placement?)

    private class PendingClick(val player: DPlayer, val entityId: Int, val type: ClickType)

    private var rootPosition = positionController.getPosition()
    private val viewers = mutableSetOf<DPlayer>()
    private val sent = mutableMapOf<DPlayer, MutableMap<Int, Sent>>()

    private val clicks = ConcurrentLinkedQueue<PendingClick>()
    private val hovered = mutableMapOf<DPlayer, Interactive>()
    private val lastRightClick = mutableMapOf<DPlayer, Pair<Long, Interactive>>()
    private val interactionTargets = mutableMapOf<Int, Interactive>()
    private val interactionPlacements = mutableMapOf<Int, Placement>()
    private var tickCount = 0L

    fun getViewers(): Collection<DPlayer> = viewers
    fun getRootPosition(): DLocation = rootPosition

    /** What [viewer] is currently looking at, if it is interactive and within range. */
    fun getHovered(viewer: DPlayer): Interactive? = hovered[viewer]

    /** Queues a click on one of this UI's interaction entities; processed on the next tick. Thread safe. */
    fun queueClick(player: DPlayer, entityId: Int, type: ClickType) {
        clicks.add(PendingClick(player, entityId, type))
    }

    fun tick() {
        tickCount++
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

        val packets = DiegeticAPI.get().getPacketAPI()
        toRemove.toSet().forEach { viewer ->
            sent.remove(viewer)?.let { if (it.isNotEmpty()) packets.removeEntities(listOf(viewer), it.keys) }
            viewers.remove(viewer)
            hovered.remove(viewer)
            lastRightClick.remove(viewer)
        }
        toAdd.forEach { viewer -> if (viewers.add(viewer)) sent[viewer] = mutableMapOf() }

        // input first, so state changes show in this tick's render
        processClicks()
        updateHover()

        // render once per group of viewers that should see the same thing, then diff per viewer
        val groups: Map<Set<Interactive>, List<DPlayer>> = when (hoverMode) {
            HoverMode.PER_VIEWER -> viewers.groupBy { viewer -> hovered[viewer]?.let { setOf(it) } ?: emptySet() }
            HoverMode.SHARED -> if (viewers.isEmpty()) emptyMap() else mapOf(hovered.values.toSet() to viewers.toList())
        }
        val placements = mutableMapOf<Int, Placement>()
        val targets = mutableMapOf<Int, Interactive>()
        groups.forEach { (hoveredTargets, group) ->
            val rendered = render(RenderContext(group.singleOrNull(), hoveredTargets))
            rendered.forEach { if (it is RenderedElement.Interaction) targets[it.entityId] = it.target }
            group.forEach { viewer -> diff(viewer, rendered, moved, placements) }
        }
        updateRegistry(targets)
        interactionPlacements.clear()
        interactionPlacements.putAll(placements)
    }

    fun destroy() {
        val packets = DiegeticAPI.get().getPacketAPI()
        sent.forEach { (viewer, entities) -> if (entities.isNotEmpty()) packets.removeEntities(listOf(viewer), entities.keys) }
        sent.clear()
        updateRegistry(emptyMap())
    }

    // ---- rendering and diffing ----

    private fun render(context: RenderContext): List<RenderedElement> {
        val rendered = LinkedList<RenderedElement>()
        element.render(rendered, Matrix4f(), context)
        // a passenger can't be offset from its vehicle, so riding UIs aren't clickable
        return if (parentEntity == null) rendered else rendered.filter { it !is RenderedElement.Interaction }
    }

    /** Sends [viewer] only what changed since they last received this UI. */
    private fun diff(viewer: DPlayer, rendered: List<RenderedElement>, moved: Boolean, placements: MutableMap<Int, Placement>) {
        val packets = DiegeticAPI.get().getPacketAPI()
        val target = listOf(viewer)
        val last = sent.getOrPut(viewer) { mutableMapOf() }
        val stale = last.keys.toMutableSet()
        var entitiesChanged = false

        rendered.forEach { element ->
            stale.remove(element.entityId)
            val placement = (element as? RenderedElement.Interaction)?.let { interaction ->
                placements.getOrPut(interaction.entityId) { placementOf(interaction) }
            }
            val previous = last.put(element.entityId, Sent(element, placement))

            when {
                previous == null -> {
                    spawn(target, element, placement)
                    entitiesChanged = true
                }

                // switched entity type (item, text or interaction): recreate it
                previous.element::class != element::class -> {
                    packets.removeEntities(target, listOf(element.entityId))
                    spawn(target, element, placement)
                    entitiesChanged = true
                }

                else -> updateExisting(target, previous, element, placement, moved && parentEntity == null)
            }
        }

        if (stale.isNotEmpty()) {
            packets.removeEntities(target, stale)
            stale.forEach(last::remove)
            entitiesChanged = true
        }

        if (parentEntity != null && entitiesChanged) {
            packets.setEntityPassengers(target, parentEntity.id(), last.keys)
        }
    }

    /** Sends only what changed between [previous] and [current]. */
    private fun updateExisting(target: List<DPlayer>, previous: Sent, current: RenderedElement, placement: Placement?, moved: Boolean) {
        val packets = DiegeticAPI.get().getPacketAPI()
        val old = previous.element

        if (current is RenderedElement.Interaction) {
            val before = previous.placement
            if (placement != null && before != null) {
                if (placement.position != before.position) packets.moveEntity(target, current.entityId, placement.location())
                if (placement.width != before.width || placement.height != before.height) {
                    packets.updateInteractionSize(target, current.entityId, placement.width, placement.height)
                }
            }
            return
        }

        if (old.offset != current.offset) {
            packets.updateDisplayOffset(target, current.entityId, current.offset, interpolationDuration)
        }

        when {
            current is RenderedElement.Item && old is RenderedElement.Item && current.item != old.item ->
                packets.updateItemDisplay(target, current.entityId, current.item)
            current is RenderedElement.Text && old is RenderedElement.Text &&
                (current.text != old.text || current.options != old.options) ->
                packets.updateTextDisplay(target, current.entityId, current.text, current.options)
        }

        if (moved) packets.moveEntity(target, current.entityId, rootPosition)
    }

    private fun spawn(target: List<DPlayer>, element: RenderedElement, placement: Placement?) {
        val packets = DiegeticAPI.get().getPacketAPI()
        when (element) {
            is RenderedElement.Item ->
                packets.spawnItemDisplay(target, element.entityId, element.item, rootPosition, element.offset, teleportDuration)
            is RenderedElement.Text ->
                packets.spawnTextDisplay(target, element.entityId, element.text, rootPosition, element.offset, teleportDuration, element.options)
            is RenderedElement.Interaction -> placement?.let {
                packets.spawnInteraction(target, element.entityId, it.location(), it.width, it.height)
            }
        }
    }

    /** The world-space hitbox around an interaction's rectangle: an axis-aligned box resting on its lowest point. */
    private fun placementOf(interaction: RenderedElement.Interaction): Placement {
        val rotation = rootPosition.orientation()
        val root = rootPosition.position()
        val min = Vector3f(Float.POSITIVE_INFINITY)
        val max = Vector3f(Float.NEGATIVE_INFINITY)
        for ((cx, cy) in listOf(-0.5f to -0.5f, 0.5f to -0.5f, -0.5f to 0.5f, 0.5f to 0.5f)) {
            val corner = interaction.offset.transformPosition(Vector3f(cx, cy, 0f)).rotate(rotation).add(root)
            min.min(corner)
            max.max(corner)
        }
        val width = maxOf(max.x - min.x, max.z - min.z, MIN_HITBOX_SIZE)
        val height = maxOf(max.y - min.y, MIN_HITBOX_SIZE)
        return Placement(Vector3f((min.x + max.x) / 2f, min.y, (min.z + max.z) / 2f), width, height)
    }

    private fun Placement.location(): DLocation = DLocation.StaticLocation(rootPosition.world(), Vector3f(position), 0f, 0f)

    private fun updateRegistry(targets: Map<Int, Interactive>) {
        (interactionTargets.keys - targets.keys).forEach(InteractionRegistry::unregister)
        (targets.keys - interactionTargets.keys).forEach { InteractionRegistry.register(it, this) }
        interactionTargets.clear()
        interactionTargets.putAll(targets)
    }

    // ---- interaction ----

    /** The nearest interactive target along [player]'s view, if any, however far away. */
    fun raycast(player: DPlayer): Hit? = element.hitTest(Ray.view(player, rootPosition), Matrix4f())

    /** How far [player] can hover and click this UI: their own reach, capped by [interactionRange]. */
    fun reachOf(player: DPlayer): Float = minOf(player.interactionRange(), interactionRange)

    /** Distance from [player]'s eye to the nearest point of an interaction entity's hitbox. */
    private fun distanceToHitbox(player: DPlayer, placement: Placement): Float {
        val eye = Vector3f(player.location().position()).add(0f, player.eyeHeight(), 0f)
        val half = placement.width / 2f
        val nearest = Vector3f(
            eye.x.coerceIn(placement.position.x - half, placement.position.x + half),
            eye.y.coerceIn(placement.position.y, placement.position.y + placement.height),
            eye.z.coerceIn(placement.position.z - half, placement.position.z + half)
        )
        return eye.distance(nearest)
    }

    private fun updateHover() {
        if (interactionTargets.isEmpty()) {
            hovered.clear()
            return
        }
        viewers.forEach { viewer ->
            val hit = raycast(viewer)?.takeIf { it.distance <= reachOf(viewer) && it.target.interactionEnabled }
            if (hit == null) hovered.remove(viewer) else hovered[viewer] = hit.target
        }
    }

    private fun processClicks() {
        val handled = mutableSetOf<Pair<DPlayer, ClickType>>()
        while (true) {
            val click = clicks.poll() ?: break
            // one click per player and button per tick: the client can send duplicates for one press
            if (click.player !in viewers || !handled.add(click.player to click.type)) continue

            // clicks must be within the player's reach, so a modified client can't click from afar
            val reach = reachOf(click.player) + CLICK_REACH_TOLERANCE
            val hit = raycast(click.player)
            val target = when {
                hit != null -> hit.target.takeIf { hit.distance <= reach }
                // the view just missed the panel but hit the (larger) hitbox: use the hitbox's widget
                else -> interactionPlacements[click.entityId]
                    ?.takeIf { distanceToHitbox(click.player, it) <= reach }
                    ?.let { interactionTargets[click.entityId] }
            } ?: continue
            if (!target.interactionEnabled) continue

            val isDrag = click.type == ClickType.RIGHT && lastRightClick[click.player]?.let { (tick, previous) ->
                tickCount - tick <= DRAG_WINDOW_TICKS && previous === target
            } == true
            if (click.type == ClickType.RIGHT) lastRightClick[click.player] = tickCount to target

            val precise = hit?.takeIf { it.target === target }
            val event = ClickEvent(
                click.player, click.type, this,
                precise?.x, precise?.y, precise?.width ?: 0f, precise?.height ?: 0f, isDrag
            )
            try {
                if (isDrag) target.drag(event) else target.click(event)
            } catch (e: Exception) {
                System.err.println("Diegetic: click handler threw for ${click.player.name()}")
                e.printStackTrace()
            }
        }
    }
}

/** Compares locations by value, since [DLocation] implementations need not implement equals. */
private fun DLocation.isSameAs(other: DLocation) =
    this === other || (world() == other.world() &&
        position() == other.position() &&
        yaw() == other.yaw() &&
        pitch() == other.pitch())
