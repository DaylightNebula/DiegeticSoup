package dsh.diegetic.flex

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.interaction.ClickEvent
import dsh.diegetic.interaction.ClickType
import dsh.diegetic.interaction.Interactive
import dsh.diegetic.interaction.RenderContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.roundToInt

/**
 * A box players can click. Left and right clicks are reported separately; [hoverBackground] is drawn
 * while a player looks at it (per viewer or shared, see the controller's hover mode).
 *
 * Widget state is shared by everyone viewing the UI; handlers receive the player who clicked.
 */
sealed class FlexInteractiveBox<T : FlexInteractiveBox<T>> : FlexBox<T>(), Interactive {
    var hoverBackground: Int? = null
        private set
    var disabledBackground: Int? = null
        private set
    var disabled: Boolean = false
        private set

    private var clickHandler: Consumer<ClickEvent>? = null
    private var leftClickHandler: Consumer<ClickEvent>? = null
    private var rightClickHandler: Consumer<ClickEvent>? = null

    /** Entity ID of the invisible hitbox that makes this clickable, allocated on first use. */
    val interactionEntityId by lazy { DiegeticAPI.get().nextEntityID() }

    override val interactionEnabled: Boolean get() = !disabled

    /** Called for left and right clicks. */
    fun onClick(handler: Consumer<ClickEvent>): T { clickHandler = handler; return self }
    fun onLeftClick(handler: Consumer<ClickEvent>): T { leftClickHandler = handler; return self }
    fun onRightClick(handler: Consumer<ClickEvent>): T { rightClickHandler = handler; return self }

    fun hoverBackground(argb: Int?): T { hoverBackground = argb; return self }
    fun hoverBackground(argb: Long): T = hoverBackground(argb.toInt())
    fun disabledBackground(argb: Int?): T { disabledBackground = argb; return self }
    fun disabledBackground(argb: Long): T = disabledBackground(argb.toInt())
    fun disabled(disabled: Boolean): T { this.disabled = disabled; return self }

    override fun resolvedBackground(context: RenderContext): Int? = when {
        disabled -> disabledBackground ?: background
        context.isHovered(this) -> hoverBackground ?: background
        else -> background
    }

    override fun click(event: ClickEvent) {
        if (disabled) return
        handleClick(event)
        when (event.type) {
            ClickType.LEFT -> leftClickHandler?.accept(event)
            ClickType.RIGHT -> rightClickHandler?.accept(event)
        }
        clickHandler?.accept(event)
    }

    /** Widget behaviour that runs before the user's handlers, such as flipping a toggle. */
    internal open fun handleClick(event: ClickEvent) {}
}

/** A clickable box, laid out like any other container. */
class FlexButton : FlexInteractiveBox<FlexButton>() {
    companion object {
        @JvmStatic fun create() = FlexButton()
        @JvmStatic fun create(label: String) = FlexButton().child(FlexText.create(label))
        @JvmStatic fun create(label: Component) = FlexButton().child(FlexText.create(label))
        @JvmStatic fun create(label: String, onClick: Consumer<ClickEvent>) = create(label).onClick(onClick)
    }

    init {
        direction(FlexDirection.ROW)
        justifyContent(JustifyContent.CENTER)
        alignItems(AlignItems.CENTER)
        padding(3f, 8f)
        background(0xC0303030.toInt())
        hoverBackground(0xC0505050.toInt())
        disabledBackground(0x80202020)
    }
}

/** A checkbox: a check indicator followed by its children (usually a label). Clicking flips it. */
class FlexToggle : FlexInteractiveBox<FlexToggle>() {
    companion object {
        const val INDICATOR_SIZE = 10f
        @JvmStatic fun create() = FlexToggle()
        @JvmStatic fun create(label: String) = FlexToggle().child(FlexText.create(label))
        @JvmStatic fun create(label: Component) = FlexToggle().child(FlexText.create(label))
    }

    var checked: Boolean = false
        private set
    var checkedColor: Int = 0xFF3BA55C.toInt()
        private set
    var uncheckedColor: Int = 0xFF4F545C.toInt()
        private set

    /** The check box drawn before the children. */
    val indicator: FlexContainer = FlexContainer.row()
        .size(INDICATOR_SIZE, INDICATOR_SIZE)
        .justifyContent(JustifyContent.CENTER)
        .alignItems(AlignItems.CENTER)
    private val mark = FlexText.create(Component.text("✔", NamedTextColor.WHITE))
    private var changeHandler: BiConsumer<ClickEvent, Boolean>? = null

    init {
        direction(FlexDirection.ROW)
        alignItems(AlignItems.CENTER)
        gap(4)
        padding(2)
        hoverBackground(0x30FFFFFF)
        insertChild(0, indicator)
        refresh()
    }

    fun checked(checked: Boolean): FlexToggle { this.checked = checked; refresh(); return this }
    fun checkedColor(argb: Int): FlexToggle { checkedColor = argb; refresh(); return this }
    fun checkedColor(argb: Long): FlexToggle = checkedColor(argb.toInt())
    fun uncheckedColor(argb: Int): FlexToggle { uncheckedColor = argb; refresh(); return this }
    fun uncheckedColor(argb: Long): FlexToggle = uncheckedColor(argb.toInt())
    fun onChange(handler: BiConsumer<ClickEvent, Boolean>): FlexToggle { changeHandler = handler; return this }

    override fun handleClick(event: ClickEvent) {
        checked = !checked
        refresh()
        changeHandler?.accept(event, checked)
    }

    private fun refresh() {
        indicator.background(if (checked) checkedColor else uncheckedColor)
        indicator.clearChildren()
        if (checked) indicator.child(mark)
    }
}

/** How a [FlexRadioGroup] draws its options. */
enum class RadioStyle {
    /** A selection dot before each option, stacked in a column. */
    DOTS,
    /** Options drawn as tabs in a row, the selected one highlighted. */
    TABS
}

/** One choice out of several. Clicking an option selects it and deselects the rest. */
class FlexRadioGroup<V> : FlexBox<FlexRadioGroup<V>>() {
    companion object {
        @JvmStatic fun <V> create() = FlexRadioGroup<V>()
    }

    var selected: V? = null
        private set
    var radioStyle: RadioStyle = RadioStyle.DOTS
        private set
    var selectedColor: Int = 0xFF3BA55C.toInt()
        private set
    var unselectedColor: Int = 0xFF4F545C.toInt()
        private set
    var tabBackground: Int = 0xC0303030.toInt()
        private set
    var tabSelectedBackground: Int = 0xE0306040.toInt()
        private set

    private val optionNodes = mutableListOf<FlexRadioOption<V>>()
    val options: List<FlexRadioOption<V>> get() = optionNodes
    private var changeHandler: BiConsumer<ClickEvent, V>? = null

    init {
        direction(FlexDirection.COLUMN)
        gap(2)
    }

    fun option(value: V, label: String): FlexRadioGroup<V> = option(value, FlexText.create(label))
    fun option(value: V, label: Component): FlexRadioGroup<V> = option(value, FlexText.create(label))
    fun option(value: V, content: FlexNode<*>): FlexRadioGroup<V> { addOption(value).child(content); return this }

    internal fun addOption(value: V): FlexRadioOption<V> {
        val option = FlexRadioOption(this, value)
        optionNodes += option
        child(option)
        option.refresh()
        return option
    }

    fun selected(value: V?): FlexRadioGroup<V> { selected = value; refresh(); return this }
    fun onChange(handler: BiConsumer<ClickEvent, V>): FlexRadioGroup<V> { changeHandler = handler; return this }

    /** Switches between dots and tabs; tabs also lay the options out in a row. */
    fun style(style: RadioStyle): FlexRadioGroup<V> {
        radioStyle = style
        if (style == RadioStyle.TABS) direction(FlexDirection.ROW).gap(0) else direction(FlexDirection.COLUMN).gap(2)
        refresh()
        return this
    }

    fun selectedColor(argb: Long): FlexRadioGroup<V> { selectedColor = argb.toInt(); refresh(); return this }
    fun unselectedColor(argb: Long): FlexRadioGroup<V> { unselectedColor = argb.toInt(); refresh(); return this }
    fun tabBackground(argb: Long): FlexRadioGroup<V> { tabBackground = argb.toInt(); refresh(); return this }
    fun tabSelectedBackground(argb: Long): FlexRadioGroup<V> { tabSelectedBackground = argb.toInt(); refresh(); return this }

    internal fun select(option: FlexRadioOption<V>, event: ClickEvent) {
        if (selected == option.value) return
        selected = option.value
        refresh()
        changeHandler?.accept(event, option.value)
    }

    private fun refresh() = optionNodes.forEach { it.refresh() }
}

/** An option of a [FlexRadioGroup]: an interactive box holding the option's content. */
class FlexRadioOption<V> internal constructor(val group: FlexRadioGroup<V>, val value: V) : FlexInteractiveBox<FlexRadioOption<V>>() {
    /** The selection dot shown in [RadioStyle.DOTS]. */
    val dot: FlexContainer = FlexContainer.create().size(8, 8)

    val isSelected: Boolean get() = group.selected == value

    init {
        direction(FlexDirection.ROW)
        alignItems(AlignItems.CENTER)
    }

    override fun handleClick(event: ClickEvent) = group.select(this, event)

    internal fun refresh() {
        removeChild(dot)
        when (group.radioStyle) {
            RadioStyle.DOTS -> {
                insertChild(0, dot)
                dot.background(if (isSelected) group.selectedColor else group.unselectedColor)
                gap(4).padding(2).background(null).hoverBackground(0x30FFFFFF)
            }
            RadioStyle.TABS -> {
                gap(0).padding(3f, 8f)
                background(if (isSelected) group.tabSelectedBackground else group.tabBackground)
                hoverBackground(if (isSelected) group.tabSelectedBackground else 0xC0505050.toInt())
            }
        }
    }
}

/**
 * A horizontal slider. Clicking the track sets the value; holding right click and moving the view
 * along the track drags it.
 */
class FlexSlider : FlexNode<FlexSlider>(), Interactive {
    companion object {
        const val DEFAULT_WIDTH = 100f
        const val DEFAULT_HEIGHT = 8f
        const val MIN_WIDTH = 20f
        @JvmStatic fun create() = FlexSlider()
        @JvmStatic fun create(min: Float, max: Float, value: Float) = FlexSlider().range(min, max).value(value)
    }

    var min: Float = 0f
        private set
    var max: Float = 100f
        private set
    /** Values snap to multiples of this from [min]; 0 means continuous. */
    var step: Float = 0f
        private set
    var value: Float = 0f
        private set
    var disabled: Boolean = false
        private set

    var trackColor: Int = 0xFF2F3136.toInt()
        private set
    var fillColor: Int = 0xFF3BA55C.toInt()
        private set
    var thumbColor: Int = 0xFFB9BBBE.toInt()
        private set
    var thumbHoverColor: Int = 0xFFFFFFFF.toInt()
        private set

    private var changeHandler: BiConsumer<ClickEvent, Float>? = null

    val trackEntityId by lazy { DiegeticAPI.get().nextEntityID() }
    val fillEntityId by lazy { DiegeticAPI.get().nextEntityID() }
    val thumbEntityId by lazy { DiegeticAPI.get().nextEntityID() }
    val interactionEntityId by lazy { DiegeticAPI.get().nextEntityID() }

    override val interactionEnabled: Boolean get() = !disabled

    /** Where the value sits between [min] and [max], from 0 to 1. */
    val fraction: Float get() = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f

    fun range(min: Float, max: Float): FlexSlider {
        require(max >= min) { "max must not be below min" }
        this.min = min; this.max = max
        value = snap(value)
        return this
    }
    fun step(step: Float): FlexSlider { require(step >= 0f) { "step must not be negative" }; this.step = step; value = snap(value); return this }
    fun value(value: Float): FlexSlider { this.value = snap(value); return this }
    fun disabled(disabled: Boolean): FlexSlider { this.disabled = disabled; return this }
    fun onChange(handler: BiConsumer<ClickEvent, Float>): FlexSlider { changeHandler = handler; return this }
    fun trackColor(argb: Long): FlexSlider { trackColor = argb.toInt(); return this }
    fun fillColor(argb: Long): FlexSlider { fillColor = argb.toInt(); return this }
    fun thumbColor(argb: Long): FlexSlider { thumbColor = argb.toInt(); return this }
    fun thumbHoverColor(argb: Long): FlexSlider { thumbHoverColor = argb.toInt(); return this }

    private fun snap(raw: Float): Float {
        val clamped = raw.coerceIn(min, max)
        if (step <= 0f) return clamped
        return (min + ((clamped - min) / step).roundToInt() * step).coerceIn(min, max)
    }

    override fun click(event: ClickEvent) = setFromEvent(event)
    override fun drag(event: ClickEvent) = setFromEvent(event)

    private fun setFromEvent(event: ClickEvent) {
        if (disabled) return
        val x = event.x ?: return
        // the thumb is as wide as the slider is tall, and its centre travels between the two half-thumbs
        val thumb = event.height
        val travel = (event.width - thumb).coerceAtLeast(1f)
        val fraction = ((x - thumb / 2f) / travel).coerceIn(0f, 1f)
        val newValue = snap(min + fraction * (max - min))
        if (newValue == value) return
        value = newValue
        changeHandler?.accept(event, newValue)
    }
}
