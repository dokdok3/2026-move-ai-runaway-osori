#!/usr/bin/env bash
set -Eeuo pipefail

ACCESS_LOG="${ACCESS_LOG:-/var/log/nginx/hackathon-access.log}"
OUTPUT_DIR="${APM_OUTPUT_DIR:-/var/www/hackathon-apm}"
OUTPUT_FILE="$OUTPUT_DIR/report.json"
WINDOW_MINUTES="${APM_WINDOW_MINUTES:-15}"
MAX_LINES="${APM_MAX_LINES:-20000}"

mkdir -p "$OUTPUT_DIR"
tmp_file="$(mktemp "$OUTPUT_DIR/.report.json.XXXXXX")"
trap 'rm -f "$tmp_file"' EXIT
if [[ ! -r "$ACCESS_LOG" ]]; then
  printf '{"generatedAt":"%s","windowMinutes":%s,"available":false,"message":"APM access log is not available yet."}\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$WINDOW_MINUTES" > "$tmp_file"
  mv "$tmp_file" "$OUTPUT_FILE"; chmod 644 "$OUTPUT_FILE"; exit 0
fi

generated_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
now_epoch="$(date +%s)"
tail -n "$MAX_LINES" "$ACCESS_LOG" | awk -F '\t' -v window="$WINDOW_MINUTES" -v now="$now_epoch" -v generated_at="$generated_at" '
function q(s) { gsub(/\\/, "\\\\", s); gsub(/"/, "\\\"", s); return "\"" s "\"" }
function request(timestamp, status, duration, method, uri, requestId) {
  return sprintf("{\"timestamp\":%d,\"status\":%d,\"durationMs\":%d,\"method\":%s,\"path\":%s,\"requestId\":%s}", timestamp * 1000, status, duration * 1000, q(method), q(uri), q(requestId))
}
BEGIN {
  threshold = now - window * 60
  for (i = 0; i < window; i++) bucketOrder[i] = int((now - (window - 1 - i) * 60) / 60) * 60
}
NF >= 6 {
  timestamp = $1 + 0; status = $2 + 0; duration = $3 + 0
  if (timestamp < threshold) next
  if ($6 ~ /^\/assets\// || $6 ~ /^\/swagger-ui\// || $6 ~ /^\/v3\/api-docs/ || $6 == "/swagger-ui.html" || $6 == "/favicon.ico" || $6 == "/logo.png" || $6 == "/deploy-status.json") next
  bucket = int(timestamp / 60) * 60
  requests++; elapsed += duration; req[bucket]++; endpoint[$6]++
  if (status >= 200 && status < 300) { successes++; success[bucket]++ }
  if ($6 ~ /^\/api\// && apiCount < 30) api[apiCount++] = request(timestamp, status, duration, $5, $6, $7)
  if (status >= 400 && status < 500) { clientErrors++; c4[bucket]++ }
  if (status >= 400 && errorCount < 20) error[errorCount++] = request(timestamp, status, duration, $5, $6, $7)
  if (status >= 500) { serverErrors++; c5[bucket]++ }
  if (duration >= 1 && slowCount < 20) slow[slowCount++] = request(timestamp, status, duration, $5, $6, $7)
}
END {
  printf "{\"generatedAt\":\"%s\",\"windowMinutes\":%d,\"available\":true,\"requests\":%d,\"successes\":%d,\"clientErrors\":%d,\"serverErrors\":%d,\"averageResponseMs\":%d,", generated_at, window, requests, successes, clientErrors, serverErrors, requests ? elapsed / requests * 1000 : 0
  printf "\"series\":["
  for (i = 0; i < window; i++) { b = bucketOrder[i]; printf "%s{\"timestamp\":%d,\"requests\":%d,\"successes\":%d,\"clientErrors\":%d,\"serverErrors\":%d}", i ? "," : "", b * 1000, req[b], success[b], c4[b], c5[b] }
  printf "],\"topEndpoints\":["
  for (rank = 0; rank < 8; rank++) {
    max = 0; selected = ""
    for (path in endpoint) if (!(path in used) && endpoint[path] > max) { max = endpoint[path]; selected = path }
    if (!max) break
    printf "%s{\"path\":%s,\"requests\":%d}", rank ? "," : "", q(selected), max
    used[selected] = 1
  }
  printf "],\"recentApiRequests\":["
  for (i = apiCount - 1; i >= 0; i--) printf "%s%s", (i == apiCount - 1 ? "" : ","), api[i]
  printf "],\"recentErrors\":["
  for (i = errorCount - 1; i >= 0; i--) printf "%s%s", (i == errorCount - 1 ? "" : ","), error[i]
  printf "],\"slowRequests\":["
  for (i = slowCount - 1; i >= 0; i--) printf "%s%s", (i == slowCount - 1 ? "" : ","), slow[i]
  print "]}"
}' > "$tmp_file"

mv "$tmp_file" "$OUTPUT_FILE"
chmod 644 "$OUTPUT_FILE"

if command -v docker >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
  backend_containers="$(docker ps --filter 'name=hackathon-deploy-backend-' --quiet)"
  if [[ -n "$backend_containers" ]]; then
    application_logs="$(docker logs --since "${WINDOW_MINUTES}m" $backend_containers 2>&1 | tail -n 1000 | jq -Rsc 'split("\n") | map(select(length > 0))')"
    report_with_logs="$(mktemp "$OUTPUT_DIR/.report-with-logs.json.XXXXXX")"
    jq --argjson applicationLogs "$application_logs" '. + {applicationLogs: $applicationLogs}' "$OUTPUT_FILE" > "$report_with_logs"
    mv "$report_with_logs" "$OUTPUT_FILE"
    chmod 644 "$OUTPUT_FILE"
  fi
fi
