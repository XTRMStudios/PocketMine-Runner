#!/system/bin/sh
SERVER_DIR="$1"
echo "PocketMine setup helper"
echo "Server dir: $SERVER_DIR"
if [ -z "$SERVER_DIR" ]; then
  echo "No server dir passed"
  exit 1
fi
mkdir -p "$SERVER_DIR/plugins"
mkdir -p "$SERVER_DIR/worlds"
mkdir -p "$SERVER_DIR/logs"
if [ ! -f "$SERVER_DIR/logs/latest.log" ]; then
  echo "[PocketMine Runner] Log created" > "$SERVER_DIR/logs/latest.log"
fi
echo "Folders checked."
echo "This starter does not bundle PocketMine binaries automatically."
echo "Use this app for file setup, config, imports, and future integration."
