package com.xtrmstudios.pocketminerunner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText serverNameEdit;
    private EditText motdEdit;
    private EditText portEdit;
    private EditText maxPlayersEdit;
    private EditText worldNameEdit;
    private TextView pathText;
    private TextView dnsPathText;
    private TextView pluginsText;
    private TextView worldsText;
    private TextView consoleText;

    private File serverRoot;
    private File pluginsDir;
    private File worldsDir;
    private File logsDir;
    private File latestLogFile;
    private File propertiesFile;
    private File dnsFile;
    private File setupScriptFile;

    private ActivityResultLauncher<String[]> pluginPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupPaths();
        setupPluginPicker();
        setupButtons();

        refreshEverything();
        log("App opened.");
    }

    private void bindViews() {
        serverNameEdit = findViewById(R.id.serverNameEdit);
        motdEdit = findViewById(R.id.motdEdit);
        portEdit = findViewById(R.id.portEdit);
        maxPlayersEdit = findViewById(R.id.maxPlayersEdit);
        worldNameEdit = findViewById(R.id.worldNameEdit);
        pathText = findViewById(R.id.pathText);
        dnsPathText = findViewById(R.id.dnsPathText);
        pluginsText = findViewById(R.id.pluginsText);
        worldsText = findViewById(R.id.worldsText);
        consoleText = findViewById(R.id.consoleText);
    }

    private void setupPaths() {
        serverRoot = new File(getFilesDir(), "pocketmine/server");
        pluginsDir = new File(serverRoot, "plugins");
        worldsDir = new File(serverRoot, "worlds");
        logsDir = new File(serverRoot, "logs");
        latestLogFile = new File(logsDir, "latest.log");
        propertiesFile = new File(serverRoot, "server.properties");
        dnsFile = new File(serverRoot, "server_dns.conf");
        setupScriptFile = new File(serverRoot, "pocketmine_setup.sh");
    }

    private void setupButtons() {
        Button createServerButton = findViewById(R.id.createServerButton);
        Button savePropsButton = findViewById(R.id.savePropsButton);
        Button runSetupButton = findViewById(R.id.runSetupButton);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button importPluginButton = findViewById(R.id.importPluginButton);
        Button listPluginsButton = findViewById(R.id.listPluginsButton);
        Button createWorldButton = findViewById(R.id.createWorldButton);
        Button deleteWorldButton = findViewById(R.id.deleteWorldButton);
        Button listWorldsButton = findViewById(R.id.listWorldsButton);
        Button showLogButton = findViewById(R.id.showLogButton);
        Button clearConsoleButton = findViewById(R.id.clearConsoleButton);

        createServerButton.setOnClickListener(v -> createServerFiles());
        savePropsButton.setOnClickListener(v -> saveServerProperties());
        runSetupButton.setOnClickListener(v -> runSetupScript());
        refreshButton.setOnClickListener(v -> refreshEverything());
        importPluginButton.setOnClickListener(v -> openPluginPicker());
        listPluginsButton.setOnClickListener(v -> showPlugins());
        createWorldButton.setOnClickListener(v -> createWorld());
        deleteWorldButton.setOnClickListener(v -> deleteWorld());
        listWorldsButton.setOnClickListener(v -> showWorlds());
        showLogButton.setOnClickListener(v -> showLatestLog());
        clearConsoleButton.setOnClickListener(v -> consoleText.setText(""));
    }

    private void setupPluginPicker() {
        pluginPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) {
                        log("Plugin import cancelled.");
                        return;
                    }
                    importPluginFromUri(uri);
                }
        );
    }

    private void openPluginPicker() {
        ensureServerLayout();
        pluginPickerLauncher.launch(new String[]{"application/octet-stream", "*/*"});
    }

    private void refreshEverything() {
        pathText.setText("Server: " + serverRoot.getAbsolutePath());
        dnsPathText.setText("DNS file: " + dnsFile.getAbsolutePath());
        ensureServerLayout();
        loadPropertiesIntoInputs();
        showPlugins();
        showWorlds();
    }

    private void ensureServerLayout() {
        try {
            mkdirsOrThrow(serverRoot);
            mkdirsOrThrow(pluginsDir);
            mkdirsOrThrow(worldsDir);
            mkdirsOrThrow(logsDir);
            if (!propertiesFile.exists()) {
                writeText(propertiesFile, defaultServerProperties());
            }
            if (!dnsFile.exists()) {
                copyAssetToFile("server_dns.conf", dnsFile);
            }
            if (!setupScriptFile.exists()) {
                copyAssetToFile("pocketmine_setup.sh", setupScriptFile);
                setupScriptFile.setExecutable(true);
            }
            if (!latestLogFile.exists()) {
                appendLogLine("[PocketMine Runner] latest.log created");
            }
        } catch (Exception e) {
            log("Layout error: " + e.getMessage());
        }
    }

    private void createServerFiles() {
        try {
            ensureServerLayout();
            saveServerProperties();

            File pluginReadme = new File(pluginsDir, "PUT_PLUGINS_HERE.txt");
            if (!pluginReadme.exists()) {
                writeText(pluginReadme,
                        "Put PocketMine plugin PHAR files here.\n" +
                                "Imported files from the app will be copied here.\n");
            }

            File worldReadme = new File(worldsDir, "WORLDS_GO_HERE.txt");
            if (!worldReadme.exists()) {
                writeText(worldReadme,
                        "Each subfolder here is treated as a world folder placeholder.\n");
            }

            log("Server structure created.");
            Toast.makeText(this, "Server created", Toast.LENGTH_SHORT).show();
            refreshEverything();
        } catch (Exception e) {
            log("Create failed: " + e.getMessage());
        }
    }

    private void saveServerProperties() {
        try {
            ensureServerLayout();
            writeText(propertiesFile, buildServerPropertiesFromInputs());
            appendLogLine("Saved server.properties");
            log("Saved server.properties");
        } catch (Exception e) {
            log("Save failed: " + e.getMessage());
        }
    }

    private void loadPropertiesIntoInputs() {
        if (!propertiesFile.exists()) {
            return;
        }
        try {
            String content = readText(propertiesFile);
            serverNameEdit.setText(getValue(content, "server-name", "My PocketMine Server"));
            motdEdit.setText(getValue(content, "motd", "A PocketMine server on Android"));
            portEdit.setText(getValue(content, "server-port", "19132"));
            maxPlayersEdit.setText(getValue(content, "max-players", "20"));
        } catch (Exception e) {
            log("Load props failed: " + e.getMessage());
        }
    }

    private void importPluginFromUri(Uri uri) {
        try {
            ensureServerLayout();
            String fileName = guessFileName(uri);
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "imported-plugin.phar";
            }
            File target = new File(pluginsDir, sanitizeName(fileName));
            copyUriToFile(uri, target);
            appendLogLine("Imported plugin: " + target.getName());
            log("Imported plugin to: " + target.getAbsolutePath());
            showPlugins();
        } catch (Exception e) {
            log("Plugin import failed: " + e.getMessage());
        }
    }

    private void showPlugins() {
        ensureServerLayout();
        File[] files = pluginsDir.listFiles();
        if (files == null || files.length == 0) {
            pluginsText.setText("Plugins: none yet");
            return;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        StringBuilder sb = new StringBuilder();
        sb.append("Plugins folder: ").append(pluginsDir.getAbsolutePath()).append("\n\n");
        for (File file : files) {
            sb.append(file.isDirectory() ? "[DIR] " : "[FILE] ")
                    .append(file.getName())
                    .append("\n");
        }
        pluginsText.setText(sb.toString().trim());
    }

    private void createWorld() {
        try {
            ensureServerLayout();
            String worldName = sanitizeName(worldNameEdit.getText().toString().trim());
            if (worldName.isEmpty()) {
                worldName = "world";
            }
            File worldDir = new File(worldsDir, worldName);
            mkdirsOrThrow(worldDir);
            writeText(new File(worldDir, "levelname.txt"), worldName + "\n");
            writeText(new File(worldDir, "world.meta"), "generator=default\ncreated_by=PocketMine Runner v3\n");
            appendLogLine("Created world folder: " + worldName);
            log("World created: " + worldName);
            showWorlds();
        } catch (Exception e) {
            log("Create world failed: " + e.getMessage());
        }
    }

    private void deleteWorld() {
        try {
            ensureServerLayout();
            String worldName = sanitizeName(worldNameEdit.getText().toString().trim());
            if (worldName.isEmpty()) {
                log("Enter a world name to delete.");
                return;
            }
            File worldDir = new File(worldsDir, worldName);
            if (!worldDir.exists()) {
                log("World not found: " + worldName);
                return;
            }
            deleteRecursive(worldDir);
            appendLogLine("Deleted world folder: " + worldName);
            log("World deleted: " + worldName);
            showWorlds();
        } catch (Exception e) {
            log("Delete world failed: " + e.getMessage());
        }
    }

    private void showWorlds() {
        ensureServerLayout();
        File[] files = worldsDir.listFiles();
        if (files == null || files.length == 0) {
            worldsText.setText("Worlds: none yet");
            return;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        StringBuilder sb = new StringBuilder();
        sb.append("Worlds folder: ").append(worldsDir.getAbsolutePath()).append("\n\n");
        for (File file : files) {
            if (file.isDirectory()) {
                sb.append("- ").append(file.getName()).append("\n");
            }
        }
        worldsText.setText(sb.toString().trim());
    }

    private void runSetupScript() {
        try {
            ensureServerLayout();
            Process process = new ProcessBuilder("sh", setupScriptFile.getAbsolutePath(), serverRoot.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            log("Running setup script...");
            while ((line = reader.readLine()) != null) {
                log(line);
                appendLogLine(line);
            }
            int code = process.waitFor();
            log("Setup finished with code " + code);
        } catch (Exception e) {
            log("Run failed: " + e.getMessage());
            log("Some Android devices restrict shell use in normal apps.");
        }
    }

    private void showLatestLog() {
        try {
            ensureServerLayout();
            String content = readText(latestLogFile);
            log("--- latest.log ---");
            log(content.trim().isEmpty() ? "(empty)" : content.trim());
        } catch (Exception e) {
            log("Show log failed: " + e.getMessage());
        }
    }

    private String buildServerPropertiesFromInputs() {
        String serverName = safe(serverNameEdit.getText().toString(), "My PocketMine Server");
        String motd = safe(motdEdit.getText().toString(), "A PocketMine server on Android");
        String port = safe(portEdit.getText().toString(), "19132");
        String maxPlayers = safe(maxPlayersEdit.getText().toString(), "20");

        return "motd=" + motd + "\n" +
                "server-name=" + serverName + "\n" +
                "server-port=" + port + "\n" +
                "max-players=" + maxPlayers + "\n" +
                "white-list=off\n" +
                "announce-player-achievements=on\n" +
                "spawn-protection=16\n" +
                "enable-query=on\n" +
                "enable-rcon=off\n" +
                "view-distance=8\n" +
                "allow-flight=on\n";
    }

    private String defaultServerProperties() {
        return buildServerPropertiesFromInputs();
    }

    private void appendLogLine(String message) throws IOException {
        mkdirsOrThrow(logsDir);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        try (FileOutputStream fos = new FileOutputStream(latestLogFile, true)) {
            fos.write(("[" + timestamp + "] " + message + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private void copyAssetToFile(String assetName, File target) throws IOException {
        try (InputStream in = getAssets().open(assetName)) {
            copyStreamToFile(in, target);
        }
    }

    private void copyUriToFile(Uri uri, File target) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Could not open file picker stream.");
            copyStreamToFile(in, target);
        }
    }

    private void copyStreamToFile(InputStream in, File target) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        try (FileOutputStream fos = new FileOutputStream(target, false)) {
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
    }

    private String readText(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = readAllBytes(fis);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }


    private byte[] readAllBytes(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private void writeText(File file, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String getValue(String content, String key, String fallback) {
        String[] lines = content.split("\\n");
        for (String line : lines) {
            if (line.startsWith(key + "=")) {
                return line.substring((key + "=").length()).trim();
            }
        }
        return fallback;
    }

    private String safe(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String sanitizeName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._ -]", "_");
    }

    private String guessFileName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (last == null) return null;
        int slash = last.lastIndexOf('/');
        return slash >= 0 ? last.substring(slash + 1) : last;
    }

    private void mkdirsOrThrow(File dir) throws IOException {
        if (dir.exists()) return;
        if (!dir.mkdirs()) {
            throw new IOException("Could not create " + dir.getAbsolutePath());
        }
    }

    private void deleteRecursive(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Could not delete " + file.getAbsolutePath());
        }
    }

    private void log(String message) {
        String current = consoleText.getText().toString();
        if (!current.isEmpty()) {
            current += "\n";
        }
        current += message;
        consoleText.setText(current);
    }
}
