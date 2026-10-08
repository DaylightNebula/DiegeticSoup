package dsh.diegetic.test.suite

import dsh.diegetic.flex.*

/**
 * Renders [flexCases] as an HTML page that uses real CSS flexbox, so a browser can produce the
 * reference layout. Each node gets `data-node="<case>:<index>"` in pre-order.
 */
object FlexHtml {
    private fun length(value: Length) = when (value) {
        Length.Auto -> "auto"
        is Length.Px -> "${value.value}px"
        is Length.Percent -> "${value.value}%"
    }

    private fun kebab(name: String) = name.lowercase().replace('_', '-')
        .replace("no-wrap", "nowrap").replace("flex-start", "flex-start")

    private fun css(node: FlexNode<*>): String {
        val s = node.style
        val m = s.margin
        val parts = mutableListOf(
            "box-sizing:border-box",
            "width:${length(s.width)}", "height:${length(s.height)}",
            "min-width:${if (s.minWidth == Length.Auto) "auto" else length(s.minWidth)}",
            "min-height:${if (s.minHeight == Length.Auto) "auto" else length(s.minHeight)}",
            "max-width:${if (s.maxWidth == Length.Auto) "none" else length(s.maxWidth)}",
            "max-height:${if (s.maxHeight == Length.Auto) "none" else length(s.maxHeight)}",
            "flex:${s.flexGrow} ${s.flexShrink} ${length(s.flexBasis)}",
            "align-self:${kebab(s.alignSelf.name)}",
            "margin:${m.top}px ${m.right}px ${m.bottom}px ${m.left}px",
        )
        if (node is FlexContainer) {
            val p = s.padding
            parts += listOf(
                "display:flex",
                "flex-direction:${kebab(s.direction.name)}",
                "flex-wrap:${kebab(s.wrap.name)}",
                "justify-content:${kebab(s.justifyContent.name)}",
                "align-items:${kebab(s.alignItems.name)}",
                "align-content:${kebab(s.alignContent.name)}",
                "row-gap:${s.rowGap}px", "column-gap:${s.columnGap}px",
                "padding:${p.top}px ${p.right}px ${p.bottom}px ${p.left}px",
            )
        }
        return parts.joinToString(";")
    }

    private fun node(node: FlexNode<*>, case: String, counter: IntArray): String {
        val id = "$case:${counter[0]++}"
        return when (node) {
            is FlexBox<*> -> "<div data-node=\"$id\" style=\"${css(node)}\">" +
                node.children.joinToString("") { node(it, case, counter) } + "</div>"
            // an item's natural size is 16x16, given by its content like a replaced element
            else -> "<div data-node=\"$id\" style=\"${css(node)}\"><i style=\"display:block;width:16px;height:16px\"></i></div>"
        }
    }

    fun page(cases: List<FlexCase> = flexCases): String = buildString {
        append("<!doctype html><html><head><meta charset=\"utf-8\"><style>body{margin:0;font-size:0}")
        append(".case{position:relative;width:2000px;height:400px} .case>div{position:absolute;left:0;top:0}</style></head><body>")
        cases.forEach { append("<div class=\"case\" id=\"${it.name}\">${node(it.root, it.name, intArrayOf(0))}</div>") }
        append("</body></html>")
    }

    /** Script that returns {case: [[x, y, w, h], ...]} relative to each case's root. */
    const val EXTRACT_SCRIPT = """
        const result = {};
        document.querySelectorAll('.case').forEach(c => {
            const nodes = [...c.querySelectorAll('[data-node]')];
            const root = nodes[0].getBoundingClientRect();
            result[c.id] = nodes.map(n => { const r = n.getBoundingClientRect();
                return [r.left - root.left, r.top - root.top, r.width, r.height].map(v => Math.round(v * 100) / 100); });
        });
        JSON.stringify(result);
    """
}
