package dsh.diegetic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Installs the Claude Code skill for this library, bundled in the jar so it always matches this version.
 *
 * ```kotlin
 * ClaudeSkill.install(Path.of(".claude/skills"))   // -> .claude/skills/diegetic-soup
 * ```
 *
 * or from a shell: `java -cp diegetic-core-<version>.jar dsh.diegetic.ClaudeSkill [skills dir]`.
 */
object ClaudeSkill {
    const val NAME = "diegetic-soup"
    private const val ROOT = "/diegetic-claude"

    /** Writes the skill's files under [skillsDirectory] and returns the skill's own folder. */
    @JvmStatic
    fun install(skillsDirectory: Path): Path {
        val index = ClaudeSkill::class.java.getResourceAsStream("$ROOT/index.txt")
            ?: throw IllegalStateException("The Claude skill is missing from this jar")
        val files = index.bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        files.forEach { file ->
            val target = skillsDirectory.resolve(file)
            Files.createDirectories(target.parent)
            ClaudeSkill::class.java.getResourceAsStream("$ROOT/skills/$file")!!.use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return skillsDirectory.resolve(NAME)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val directory = Path.of(args.firstOrNull() ?: ".claude/skills")
        println("Installed the $NAME skill to ${install(directory).toAbsolutePath()}")
    }
}
