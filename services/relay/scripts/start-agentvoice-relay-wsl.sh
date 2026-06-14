#!/usr/bin/env bash
set -euo pipefail

ROOT="${AGENTVOICE_ROOT:-/mnt/e/Github/pulls/agent-voice}"
RELAY_DIR="${AGENTVOICE_RELAY_DIR:-$ROOT/services/relay}"
OPENCLAW_CONFIG_PATH="${OPENCLAW_CONFIG_PATH:-$HOME/.openclaw/openclaw.json}"
HERMES_ENV_FILE="${HERMES_ENV_FILE:-$HOME/.hermes/.env}"

if [[ -f "$HERMES_ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source <(sed 's/\r$//' "$HERMES_ENV_FILE")
  set +a
fi

ENV_FILE="$RELAY_DIR/.env"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source <(sed 's/\r$//' "$ENV_FILE")
  set +a
fi

if [[ ! -f "$OPENCLAW_CONFIG_PATH" ]]; then
  echo "OpenClaw config not found at $OPENCLAW_CONFIG_PATH" >&2
  exit 2
fi

token="$(
  node -e '
    const fs = require("fs");
    const configPath = process.argv[1];
    const cfg = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const token = cfg.gateway?.auth?.token;
    if (typeof token === "string") process.stdout.write(token);
  ' "$OPENCLAW_CONFIG_PATH" | tr -d "\r"
)"

if [[ -z "$token" ]]; then
  echo "OpenClaw gateway token is empty in $OPENCLAW_CONFIG_PATH" >&2
  exit 3
fi

export OPENCLAW_ENABLED="${OPENCLAW_ENABLED:-true}"
export OPENCLAW_GATEWAY_URL="${OPENCLAW_GATEWAY_URL:-http://127.0.0.1:18789}"
export OPENCLAW_TOKEN="$token"
export OPENCLAW_MODEL="${OPENCLAW_MODEL:-openclaw}"
export OPENCLAW_TIMEOUT_MS="${OPENCLAW_TIMEOUT_MS:-60000}"
export HERMES_ENABLED="${HERMES_ENABLED:-false}"
export HERMES_BASE_URL="${HERMES_BASE_URL:-http://127.0.0.1:8642}"
export HERMES_TOKEN="${HERMES_TOKEN:-${API_SERVER_KEY:-}}"
export HERMES_MODEL="${HERMES_MODEL:-hermes}"
export HERMES_USER="${HERMES_USER:-agentvoice-mobile}"
export HERMES_TIMEOUT_MS="${HERMES_TIMEOUT_MS:-30000}"
export HOST="${HOST:-0.0.0.0}"
export PORT="${PORT:-3001}"
export LOG_LEVEL="${LOG_LEVEL:-info}"

cd "$RELAY_DIR"
exec node dist/index.js
