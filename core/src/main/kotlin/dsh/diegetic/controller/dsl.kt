package dsh.diegetic.controller

import dsh.diegetic.elements.DSLElement
import dsh.diegetic.interaction.HoverMode
import dsh.diegetic.interop.DEntity
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController

var DSLController.viewerController: ViewerController?
    get() = currentViewerController
    set(value) { viewerController(value ?: throw IllegalArgumentException("ViewerController cannot be null")) }

var DSLController.viewers: ViewerController?
    get() = viewerController
    set(value) { viewerController = value }

var DSLController.positionController: PositionController?
    get() = currentPositionController
    set(value) { positionController(value ?: throw IllegalArgumentException("PositionController cannot be null")) }

var DSLController.position: PositionController?
    get() = positionController
    set(value) { positionController = value }

var DSLController.parentEntity: DEntity?
    get() = currentParentEntity
    set(value) { parentEntity(value) }

var DSLController.interpolationDuration: Int
    get() = currentInterpolationDuration
    set(value) { interpolationDuration(value) }

var DSLController.teleportDuration: Int
    get() = currentTeleportDuration
    set(value) { teleportDuration(value) }

var DSLController.hoverMode: HoverMode
    get() = currentHoverMode
    set(value) { hoverMode(value) }

var DSLController.interactionRange: Float
    get() = currentInteractionRange
    set(value) { interactionRange(value) }

fun DSLController.element(callback: DSLElement.() -> Unit): DSLController = element(DSLElement.create().apply(callback))

fun diegetic(autoSpawn: Boolean = true, callback: DSLController.() -> Unit): DiegeticController {
    val builder = DSLController.create().apply(callback)
    return if (autoSpawn) builder.spawn() else builder.build()
}
