package dsh.diegetic.test.suite

import dsh.diegetic.flex.Box
import dsh.diegetic.flex.FlexLayout
import kotlin.math.abs

/**
 * Reference layouts produced by a real browser for [flexCases]. Regenerate by running the
 * `flex-export-css-cases` scenario, opening the page it writes, and evaluating [FlexHtml.EXTRACT_SCRIPT].
 */
object FlexGolden {
    val boxes: Map<String, List<Box>> by lazy {
        val json = FlexGolden::class.java.getResource("/flex-css-golden.json")!!.readText()
        Regex("\"([^\"]+)\":\\[((?:\\[[^\\]]*\\],?)*)\\]").findAll(json).associate { match ->
            match.groupValues[1] to Regex("\\[([^\\]]*)\\]").findAll(match.groupValues[2]).map { box ->
                val (x, y, w, h) = box.groupValues[1].split(',').map { it.trim().toFloat() }
                Box(x, y, w, h)
            }.toList()
        }
    }
}

private fun Box.matches(other: Box, tolerance: Float = 0.5f) =
    abs(x - other.x) <= tolerance && abs(y - other.y) <= tolerance &&
        abs(width - other.width) <= tolerance && abs(height - other.height) <= tolerance

private fun Box.short() = "(%.2f, %.2f, %.2f×%.2f)".format(x, y, width, height)

val flexLayoutScenarios: List<Scenario> = flexCases.map { case ->
    Scenario("flex-css-${case.name}", needsClient = false) {
        val expected = FlexGolden.boxes[case.name] ?: throw AssertionFailed("No golden data for ${case.name}")
        val layout = FlexLayout.compute(case.root)
        val actual = preOrder(case.root).map { layout[it] ?: throw AssertionFailed("Node was not laid out") }
        check(actual.size == expected.size) { "Expected ${expected.size} nodes, laid out ${actual.size}" }
        val wrong = actual.indices.filter { !actual[it].matches(expected[it]) }
        check(wrong.isEmpty()) {
            wrong.joinToString("; ") { "node $it: got ${actual[it].short()} want ${expected[it].short()}" }
        }
    }
}
