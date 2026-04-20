package com.xtrmstudios.pocketminerunner

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.function.Consumer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var consoleView: TextView
    private var serverProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleView = findViewById(R.id.consoleText)

        val createServerButton = findViewById<Button>(R.id.btnCreateServer)
        val startServerButton = findViewById<Button>(R.id.btnStartServer)
        val showLogButton = findViewById<Button>(R.id.btnShowLog)
        val clearConsoleButton = findViewById<Button>(R.id.btnClearConsole)

        log("App opened.")

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
                val logFile = File(filesDir, "pocketmine/server/logs/latest.log")
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
    }

    private fun log(message: String) {
        runOnUiThread {
            consoleView.append("$message\n")
        }
    }
}
