package com.xtrmstudios.pocketminerunner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.function.Consumer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var consoleView: TextView
    private var serverProcess: Process? = null

    private val createResolvConfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            if (uri == null) {
                log("resolv.conf creation cancelled.")
                return@registerForActivityResult
            }

            try {
                contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(
                        """
                        nameserver 1.1.1.1
                        nameserver 8.8.8.8
                        """.trimIndent().toByteArray()
                    )
                }

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                getSharedPreferences("pm_runner", MODE_PRIVATE)
                    .edit()
                    .putString("resolv_conf_uri", uri.toString())
                    .apply()

                log("Saved resolv.conf to: $uri")
                Toast.makeText(this, "resolv.conf created.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                log("Failed to create resolv.conf: ${e.message}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleView = findViewById(R.id.consoleText)

        val createServerButton = findViewById<Button>(R.id.btnCreateServer)
        val startServerButton = findViewById<Button>(R.id.btnStartServer)
        val showLogButton = findViewById<Button>(R.id.btnShowLog)
        val clearConsoleButton = findViewById<Button>(R.id.btnClearConsole)
        val openFolderButton = findViewById<Button>(R.id.btnOpenFolder)
        val createResolvButton = findViewById<Button>(R.id.btnCreateResolv)

        log("App opened.")

        createResolvButton.setOnClickListener {
            createResolvConfLauncher.launch("resolv.conf")
        }

        createServerButton.setOnClickListener {
            thread {
                try {
                    log("Preparing server files...")
                    SetupManager.ensurePhar(this, Consumer { log(it) })
                    log("Setup finished.")
                } catch (e: Exception) {
                    log("Setup failed: ${e.message}")
                }
            }
        }

        startServerButton.setOnClickListener {
            thread {
                try {
                    if (serverProcess != null) {
                        log("Server already running.")
                        return@thread
                    }

                    SetupManager.ensurePhar(this, Consumer { log(it) })
                    serverProcess = SetupManager.startServer(this, Consumer { log(it) })

                    val reader = serverProcess!!.inputStream.bufferedReader()
                    reader.forEachLine { line ->
                        log(line)
                    }
                } catch (e: Exception) {
                    log("Start failed: ${e.message}")
                }
            }
        }

        showLogButton.setOnClickListener {
            try {
                val logFile = File(SetupManager.getPaths(this).logsDir, "latest.log")
                if (logFile.exists()) {
                    log(logFile.readText())
                } else {
                    log("No latest.log found yet.")
                }
            } catch (e: Exception) {
                log("Failed to read log: ${e.message}")
            }
        }

        clearConsoleButton.setOnClickListener {
            consoleView.text = ""
        }

        openFolderButton.setOnClickListener {
            val path = SetupManager.getPaths(this).baseDir.absolutePath
            Toast.makeText(this, path, Toast.LENGTH_LONG).show()
            log("Server folder: $path")
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            consoleView.append("$message\n")
        }
    }
}
