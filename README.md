# PocketMine Runner v3 Starter

This is a GitHub-ready Android starter project for a future PocketMine helper app.

## What this version adds
- Better main screen layout
- `server.properties` editor
- Plugin importer using Android's file picker
- Plugin folder listing
- World manager with create/delete/list tools
- Internal `latest.log` viewer
- GitHub Actions workflow that builds APKs and uploads them as artifacts
- Release uploads when you push a `v*` tag

## Important honesty note
This is still a **starter project**. It is not a fully finished one-tap Android PocketMine host yet.

A normal Android app on a non-rooted phone usually **cannot edit `/etc/resolv.conf`** directly.
That is why this project stores DNS settings inside app/server files instead:

- `app/src/main/assets/server_dns.conf`
- copied into app storage at runtime as:
  - `getFilesDir()/pocketmine/server/server_dns.conf`

So yes: this avoids needing an SD card.

## App storage used
The app creates files inside internal app storage like:

- `.../files/pocketmine/server/`
- `.../files/pocketmine/server/plugins/`
- `.../files/pocketmine/server/worlds/`
- `.../files/pocketmine/server/logs/latest.log`
- `.../files/pocketmine/server/server.properties`
- `.../files/pocketmine/server/server_dns.conf`

## What the buttons do
- **Create Server**: makes the folder structure and starter files
- **Save Props**: writes `server.properties`
- **Run Setup**: runs the included shell helper if the device allows it
- **Import Plugin**: opens Android's file picker and copies the chosen file into `plugins`
- **List Plugins**: shows plugin files in the folder
- **Create World**: makes a world folder placeholder
- **Delete World**: removes the named world folder
- **List Worlds**: shows current world folders
- **Show Log**: displays `latest.log`

## Upload from Windows Command Prompt
1. Extract the zip on your PC.
2. Create a new empty GitHub repo in your browser.
3. Open the extracted folder.
4. Click the path bar in File Explorer, type `cmd`, and press Enter.
5. Run:

```bat
git init
git branch -M main
git add .
git commit -m "Initial PocketMine Runner v3"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

Replace the GitHub URL with your real repo URL.

## Trigger a release APK build
After the first push, run this from Command Prompt in the project folder:

```bat
git tag v1.0.0
git push origin v1.0.0
```

Then check:
- **Actions** for the build logs
- **Artifacts** for the APK files
- **Releases** for tagged build uploads

## Notes about building
This repo uses a GitHub Actions workflow in:

- `.github/workflows/android.yml`

It builds:
- debug APK
- release APK

The release APK in this starter is **unsigned** by default unless you later add signing secrets.

## Good next upgrades
- Real in-app terminal screen
- Start/stop server process integration with Termux or a foreground service
- Server console streaming
- World zip import/export
- Plugin delete button
- Signed release workflow with GitHub Secrets
