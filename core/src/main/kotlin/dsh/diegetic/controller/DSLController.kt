package dsh.diegetic.controller

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.DiegeticDsl
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.interaction.HoverMode
import dsh.diegetic.interop.DEntity
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController

@DiegeticDsl
class DSLController private constructor() {
    companion object {
        @JvmStatic
        fun create() = DSLController()
    }

    internal var currentViewerController: ViewerController? = null
        private set
    internal var currentPositionController: PositionController? = null
        private set
    internal var currentElement: DiegeticElement? = null
        private set
    internal var currentParentEntity: DEntity? = null
        private set
    internal var currentInterpolationDuration: Int = DiegeticController.DEFAULT_INTERPOLATION_DURATION
        private set
    internal var currentTeleportDuration: Int = DiegeticController.DEFAULT_TELEPORT_DURATION
        private set
    internal var currentHoverMode: HoverMode = HoverMode.PER_VIEWER
        private set
    internal var currentInteractionRange: Float = DiegeticController.DEFAULT_INTERACTION_RANGE
        private set

    fun viewerController(viewerController: ViewerController): DSLController = apply { currentViewerController = viewerController }
    fun positionController(positionController: PositionController): DSLController = apply { currentPositionController = positionController }
    fun element(element: DiegeticElement): DSLController = apply { currentElement = element }
    fun parentEntity(parentEntity: DEntity?): DSLController = apply { currentParentEntity = parentEntity }

    /** Ticks the client takes to blend elements to a new offset (0 snaps). */
    fun interpolationDuration(ticks: Int): DSLController = apply { currentInterpolationDuration = ticks }

    /** Ticks the client takes to move elements to a new position, 0-59 (0 snaps). */
    fun teleportDuration(ticks: Int): DSLController = apply { currentTeleportDuration = ticks }

    /** Whether hover highlights are per viewer (default) or shared by everyone watching. */
    fun hoverMode(mode: HoverMode): DSLController = apply { currentHoverMode = mode }

    /** Caps how far away, in blocks, viewers can hover and click the UI; their own reach always applies. */
    fun interactionRange(blocks: Float): DSLController = apply { currentInteractionRange = blocks }

    fun build() = DiegeticController(
        viewerController = currentViewerController ?: throw IllegalStateException("ViewerController not initialized"),
        positionController = currentPositionController ?: throw IllegalStateException("PositionController not initialized"),
        element = currentElement ?: throw IllegalStateException("Element not initialized"),
        parentEntity = currentParentEntity,
        interpolationDuration = currentInterpolationDuration,
        teleportDuration = currentTeleportDuration,
        hoverMode = currentHoverMode,
        interactionRange = currentInteractionRange
    )

    fun spawn(): DiegeticController {
        val controller = build()
        DiegeticAPI.get().addController(controller)
        return controller
    }
}
