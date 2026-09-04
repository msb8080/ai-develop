#!/usr/bin/env bash
set -euo pipefail

config_file="${XTOKEN_CONFIG_FILE:-${XDG_CONFIG_HOME:-$HOME/.config}/opencode/opencode.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to read the local xtoken configuration" >&2
  exit 2
fi

if [[ ! -r "$config_file" ]]; then
  echo "xtoken configuration is not readable: $config_file" >&2
  exit 2
fi

base_url="$(jq -er '.provider.xtoken.options.baseURL' "$config_file")"
api_key="$(jq -er '.provider.xtoken.options.apiKey' "$config_file")"

# Spring AI appends /v1/chat/completions, while OpenCode providers commonly store a /v1 base.
base_url="${base_url%/}"
base_url="${base_url%/v1}"

export AI_ENABLED=true
export AI_PROVIDER=xtoken
export AI_BASE_URL="$base_url"
export AI_API_KEY="$api_key"
export AI_MODEL="${AI_MODEL:-gpt-5.4}"
export AI_STREAM_MODE="${AI_STREAM_MODE:-buffered}"

exec "$@"
