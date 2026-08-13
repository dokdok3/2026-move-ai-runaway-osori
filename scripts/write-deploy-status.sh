#!/usr/bin/env bash
set -Eeuo pipefail

STATUS="${1:?status is required}"
STAGE="${2:?stage is required}"
IMAGE_TAG="${3:-unknown}"
ACTIVE_COLOR="${4:-none}"
TARGET_COLOR="${5:-none}"
STARTED_AT_EPOCH="${6:-$(date +%s)}"
STATUS_DIR="${DEPLOY_STATUS_DIR:-/var/www/hackathon-deploy}"
STATUS_FILE="$STATUS_DIR/deploy-status.json"

case "$STATUS" in
  building|deploying|success|failed) ;;
  *) echo "Invalid deployment status: $STATUS" >&2; exit 1 ;;
esac

case "$STAGE" in
  test|image-build|image-ready|pull-images|start-candidate|health-check|switch-traffic|complete|failed) ;;
  *) echo "Invalid deployment stage: $STAGE" >&2; exit 1 ;;
esac

if [[ ! "$IMAGE_TAG" =~ ^([0-9a-f]{7,40}|unknown)$ ]]; then
  echo "Invalid image tag: $IMAGE_TAG" >&2
  exit 1
fi

for color in "$ACTIVE_COLOR" "$TARGET_COLOR"; do
  if [[ ! "$color" =~ ^(blue|green|none)$ ]]; then
    echo "Invalid deployment color: $color" >&2
    exit 1
  fi
done

if [[ ! "$STARTED_AT_EPOCH" =~ ^[0-9]+$ ]]; then
  echo "Invalid start time: $STARTED_AT_EPOCH" >&2
  exit 1
fi

mkdir -p "$STATUS_DIR"
temporary_file="$(mktemp "$STATUS_DIR/.deploy-status.XXXXXX")"
trap 'rm -f "$temporary_file"' EXIT

if date -u -d "@$STARTED_AT_EPOCH" '+%Y-%m-%dT%H:%M:%SZ' >/dev/null 2>&1; then
  started_at="$(date -u -d "@$STARTED_AT_EPOCH" '+%Y-%m-%dT%H:%M:%SZ')"
else
  started_at="$(date -u -r "$STARTED_AT_EPOCH" '+%Y-%m-%dT%H:%M:%SZ')"
fi
updated_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
completed_at="null"
if [[ "$STATUS" == "success" || "$STATUS" == "failed" ]]; then
  completed_at="\"$updated_at\""
fi

cat > "$temporary_file" <<EOF
{
  "status": "$STATUS",
  "stage": "$STAGE",
  "imageTag": "$IMAGE_TAG",
  "activeColor": "$ACTIVE_COLOR",
  "targetColor": "$TARGET_COLOR",
  "startedAt": "$started_at",
  "updatedAt": "$updated_at",
  "completedAt": $completed_at
}
EOF

chmod 644 "$temporary_file"
mv -f "$temporary_file" "$STATUS_FILE"
