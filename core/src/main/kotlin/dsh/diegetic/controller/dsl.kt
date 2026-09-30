package dsh.diegetic.controller

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.elements.DSLElement
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.interop.DEntity
import dsh.diegetic.position.PositionController
import dsh.diegetic.viewers.ViewerController

class ControllerDSL {
    var viewerController: ViewerController? = null
    var positionController: PositionController? = null
    var element: DiegeticElement? = null
    var parentEntity: DEntity? = null
    var autoSpawn: Boolean = true

    var viewers: ViewerController?
        get() = viewerController
        set(value) { viewerController = value }

    var position: PositionController?
        get() = positionController
        set(value) { positionController = value }

    fun element(callback: DSLElement.() -> Unit) {
        element = DSLElement().apply(callback)
    }

    fun build() = DiegeticController(
        viewerController = viewerController ?: throw IllegalStateException("ViewerController not initialized"),
        positionController = positionController ?: throw IllegalStateException("PositionController not initialized"),
        element = element ?: throw IllegalStateException("Element not initialized"),
        parentEntity = parentEntity
    )
}

fun diegetic(callback: ControllerDSL.() -> Unit): DiegeticController {
    val builder = ControllerDSL()
    builder.callback()
    val controller = builder.build()
    if (builder.autoSpawn) DiegeticAPI.get().addController(controller)
    return controller
}