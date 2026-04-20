#!/usr/bin/env sh
SERVER_DIR="$1"
mkdir -p "$SERVER_DIR/plugins" "$SERVER_DIR/worlds" "$SERVER_DIR/logs"
printf '%s\n' '[PocketMine Runner] Desktop helper ran.' > "$SERVER_DIR/logs/latest.log"
