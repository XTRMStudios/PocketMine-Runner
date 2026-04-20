package com.xtrmstudios.pocketminerunner

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Consumer

object SetupManager {

    private const val POCKETMINE_PHAR_URL =
        "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar"

    data class Paths(
        val baseDir: File,
        val configDir: File,
        val serverDir: File,
        val phpFile: File,
        val phpIni: File,
        val resolvConf: File,
        val cacertPem: File,
        val pharFile: File,
        val pluginsDir: File,
        val worldsDir: File,
        val logsDir: File
    )

    @JvmStatic
    fun getPaths(context: Context): Paths {
        val base = File(context.filesDir, "pocketmine")
        val config = File(base, "config")
        val server = File(base, "server")
        val plugins = File(server, "plugins")
        val worlds = File(server, "worlds")
        val logs = File(server, "logs")

        val phpPath = File(context.applicationInfo.nativeLibraryDir, "libphp.so")

        return Paths(
            baseDir = base,
            configDir = config,
            serverDir = server,
            phpFile = phpPath,
            phpIni = File(config, "php.ini"),
            resolvConf = File(config, "resolv.conf"),
            cacertPem = File(config, "cacert.pem"),
            pharFile = File(server, "PocketMine-MP.phar"),
            pluginsDir = plugins,
            worldsDir = worlds,
            logsDir = logs
        )
    }

    @JvmStatic
    fun ensureStructure(context: Context, log: Consumer<String>): Paths {
        val p = getPaths(context)

        listOf(
            p.baseDir,
            p.configDir,
            p.serverDir,
            p.pluginsDir,
            p.worldsDir,
            p.logsDir
        ).forEach {
            if (!it.exists()) it.mkdirs()
        }

        copyAssetIfMissing(context, "config/resolv.conf", p.resolvConf)
        copyAssetIfMissing(context, "config/php.ini", p.phpIni)
        copyAssetIfMissing(context, "config/cacert.pem", p.cacertPem)
        ensureServerProperties(p.serverDir)

        log.accept("Server dir: ${p.serverDir.absolutePath}")
        log.accept("PHP path: ${p.phpFile.absolutePath}")
        log.accept("Folders checked.")
        return p
    }

    @JvmStatic
    fun ensurePhar(context: Context, log: Consumer<String>) {
        val p = ensureStructure(context, log)

        if (!p.phpFile.exists()) {
            throw IllegalStateException(
                "Bundled PHP not found at ${p.phpFile.absolutePath}. " +
                    "Put libphp.so and the other required .so files in app/src/main/jniLibs/arm64-v8a/"
            )
        }

        p.phpFile.setReadable(true, false)
        p.phpFile.setExecutable(true, false)

        log.accept("Bundled PHP exists: ${p.phpFile.exists()}")
        log.accept("Bundled PHP canExecute(): ${p.phpFile.canExecute()}")

        if (!p.pharFile.exists()) {
            log.accept("Downloading PocketMine-MP.phar...")
            downloadFile(POCKETMINE_PHAR_URL, p.pharFile, log)
            log.accept("PHAR ready: ${p.pharFile.absolutePath}")
        } else {
            log.accept("PocketMine-MP.phar already exists.")
        }
    }

    @JvmStatic
    fun startServer(context: Context, log: Consumer<String>): Process {
        val p = getPaths(context)

        require(p.phpFile.exists()) { "Bundled PHP missing." }
        require(p.pharFile.exists()) { "PocketMine-MP.phar missing." }

        p.phpFile.setReadable(true, false)
        p.phpFile.setExecutable(true, false)

        if (!p.phpFile.canExecute()) {
            throw IllegalStateException("Bundled PHP is not executable at ${p.phpFile.absolutePath}")
        }

        val tmpDir = File(context.filesDir, "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()

        log.accept("Starting PocketMine...")
        log.accept("Working dir: ${p.serverDir.absolutePath}")
        log.accept("PHP path: ${p.phpFile.absolutePath}")
        log.accept("PHP canExecute(): ${p.phpFile.canExecute()}")
        log.accept("Temp dir: ${tmpDir.absolutePath}")

        val builder = ProcessBuilder(
            p.phpFile.absolutePath,
            "-c", p.phpIni.absolutePath,
            "-d", "sys_temp_dir=${tmpDir.absolutePath}",
            p.pharFile.absolutePath
        )

        builder.directory(p.serverDir)
        builder.redirectErrorStream(true)

        val env = builder.environment()
        env["SSL_CERT_FILE"] = p.cacertPem.absolutePath
        env["LESMI_RESOLV_CONF_DIR"] = p.resolvConf.absolutePath
        env["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir
        env["TMPDIR"] = tmpDir.absolutePath
        env["TMP"] = tmpDir.absolutePath
        env["TEMP"] = tmpDir.absolutePath

        return builder.start()
    }

    private fun copyAssetIfMissing(context: Context, assetPath: String, outFile: File) {
        if (outFile.exists()) return

        outFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun ensureServerProperties(serverDir: File) {
        val file = File(serverDir, "server.properties")
        if (file.exists()) return

        file.writeText(
            """
            motd=PocketMine Server
            server-port=19132
            max-players=20
            level-name=world
            gamemode=survival
            force-gamemode=false
            difficulty=1
            allow-cheats=false
            online-mode=true
            white-list=false
            view-distance=8
            tick-distance=4
            """.trimIndent()
        )
    }

    private fun downloadFile(url: String, target: File, log: Consumer<String>) {
        target.parentFile?.mkdirs()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 120000
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        connection.connect()

        val code = connection.responseCode
        if (code !in 200..299) {
            throw IllegalStateException("Download failed: HTTP $code for $url")
        }

        connection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }

        connection.disconnect()
        log.accept("Downloaded: ${target.name}")
    }
}
