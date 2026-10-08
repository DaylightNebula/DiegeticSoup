dependencies {
    // JOML types (Matrix4f, Vector3f, Quaternionf) are part of the public API
    api("org.joml:joml:1.10.8")
    // provided by Minestom and Paper
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
}

// Ships the Claude Code skill (claude-plugin/skills/diegetic-soup) in the jar, so ClaudeSkill.install can
// write out the instructions matching this version. Jars can't list a resource directory, so an index of the
// files is written next to them.
val skillSource = rootProject.layout.projectDirectory.dir("claude-plugin/skills")
val generatedSkill = layout.buildDirectory.dir("generated/claude-skill")
val bundleClaudeSkill = tasks.register<Sync>("bundleClaudeSkill") {
    from(skillSource) { into("diegetic-claude/skills") }
    into(generatedSkill)
    doLast {
        val root = generatedSkill.get().dir("diegetic-claude").asFile
        val files = root.resolve("skills").walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root.resolve("skills")).invariantSeparatorsPath }
            .sorted().toList()
        root.resolve("index.txt").writeText(files.joinToString("\n", postfix = "\n"))
    }
}
sourceSets.main {
    resources.srcDir(files(generatedSkill).builtBy(bundleClaudeSkill))
}
