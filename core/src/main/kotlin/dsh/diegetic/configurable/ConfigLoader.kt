package dsh.diegetic.configurable

import dsh.diegetic.DiegeticAPI
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import kotlin.io.path.Path

object ConfigLoader {

    lateinit var watcher: WatchService
    val files = mutableMapOf<String, DiegeticElementConfig>()

    fun init(path: String) {
        watcher = FileSystems.getDefault().newWatchService()
        val path = Path(path)
        path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)

        println("Loading config files from: ${path.toFile().absolutePath}")
        path.toFile().listFiles().forEach { file ->
            println("Loading config file: ${file.nameWithoutExtension}")
            if (file.isFile) {
                val config = DiegeticElementConfig.deserialize(file.readText())
                files[file.nameWithoutExtension] = config
            }
        }

        Thread {
            while (true) {
                val key = watcher.take()
                for (event in key.pollEvents()) {
                    try {
                        // load file at the events context file path
                        val filePath = event.context() as? Path ?: continue
                        val file = path.resolve(filePath).toFile()
                        if (!file.isFile) continue

                        // update stored config
                        val config = DiegeticElementConfig.deserialize(file.readText())
                        files[file.nameWithoutExtension] = config

                        // update elements using config
                        DiegeticAPI.get().getActiveControllers()
                            .mapNotNull { it.element as? ConfigurableDiegeticElement }
                            .filter { it.configId == file.nameWithoutExtension }
                            .forEach { it.updateConfig(config) }
                    } catch (e: FileNotFoundException) {}
                }
                if (!key.reset()) break
            }
        }.start()
    }
}