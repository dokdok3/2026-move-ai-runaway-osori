#!/usr/bin/env bash
set -Eeuo pipefail

IMAGE_TAG="${1:?usage: deploy-blue-green.sh IMAGE_TAG}"
BACKEND_IMAGE="${BACKEND_IMAGE:?BACKEND_IMAGE is required}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:?FRONTEND_IMAGE is required}"
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/compose.prod.yaml"
ACTIVE_COLOR_FILE="$DEPLOY_DIR/.active-color"
ACTIVE_IMAGE_FILE="$DEPLOY_DIR/.active-image-tag"
NGINX_TEMPLATE="$DEPLOY_DIR/deploy/nginx/hackathon.conf.template"
DEPLOY_STARTED_AT="${DEPLOY_STARTED_AT:-$(date +%s)}"
DEPLOY_STATUS_DIR="${DEPLOY_STATUS_DIR:-/var/www/hackathon-deploy}"
STATUS_WRITER="$DEPLOY_DIR/scripts/write-deploy-status.sh"

write_status() {
  DEPLOY_STATUS_DIR="$DEPLOY_STATUS_DIR" DEPLOY_ACTOR="${DEPLOY_ACTOR:-unknown}" DEPLOY_CHANGES="${DEPLOY_CHANGES:-}" \
    "$STATUS_WRITER" "$1" "$2" "$IMAGE_TAG" "${active_color:-none}" "${target_color:-none}" "$DEPLOY_STARTED_AT"
}

deployment_failed() {
  write_status failed failed || true
}

trap deployment_failed ERR

if [[ ! "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo "IMAGE_TAG must be a full Git commit SHA" >&2
  exit 1
fi

if [[ ! -f "$DEPLOY_DIR/.env" ]]; then
  echo "$DEPLOY_DIR/.env is missing; run scripts/setup-ec2.sh first" >&2
  exit 1
fi

active_color="none"
if [[ -f "$ACTIVE_COLOR_FILE" ]]; then
  active_color="$(<"$ACTIVE_COLOR_FILE")"
fi
previous_color="$active_color"

case "$active_color" in
  blue)
    target_color="green"
    backend_port="8082"
    frontend_port="3002"
    ;;
  green)
    target_color="blue"
    backend_port="8081"
    frontend_port="3001"
    ;;
  none)
    target_color="blue"
    backend_port="8081"
    frontend_port="3001"
    ;;
  *)
    echo "Invalid active color: $active_color" >&2
    exit 1
    ;;
esac

export IMAGE_TAG BACKEND_IMAGE FRONTEND_IMAGE
compose=(docker compose --env-file "$DEPLOY_DIR/.env" -f "$COMPOSE_FILE")
target_services=("backend-$target_color" "frontend-$target_color")

echo "Deploying $IMAGE_TAG to $target_color"
"${compose[@]}" up -d postgres redis
write_status deploying pull-images
"${compose[@]}" pull "${target_services[@]}"
write_status deploying start-candidate
"${compose[@]}" up -d --no-deps --force-recreate "${target_services[@]}"

healthy=false
write_status deploying health-check
for _ in {1..60}; do
  if curl --fail --silent "http://127.0.0.1:$backend_port/actuator/health" | grep -q '"status":"UP"' \
    && curl --fail --silent "http://127.0.0.1:$frontend_port/health" | grep -q "UP"; then
    healthy=true
    break
  fi
  sleep 2
done

if [[ "$healthy" != "true" ]]; then
  echo "Health check failed for $target_color; keeping $active_color active" >&2
  "${compose[@]}" stop "${target_services[@]}"
  exit 1
fi

candidate_config="$(mktemp)"
backup_config="$(mktemp)"
trap 'rm -f "$candidate_config" "$backup_config"' EXIT
sed \
  -e "s/{{BACKEND_PORT}}/$backend_port/g" \
  -e "s/{{FRONTEND_PORT}}/$frontend_port/g" \
  "$NGINX_TEMPLATE" > "$candidate_config"

nginx_config="/etc/nginx/conf.d/hackathon.conf"
had_previous_config=false
if sudo test -f "$nginx_config"; then
  sudo cp "$nginx_config" "$backup_config"
  had_previous_config=true
fi

sudo install -m 644 "$candidate_config" "$nginx_config"
if ! sudo nginx -t; then
  if [[ "$had_previous_config" == "true" ]]; then
    sudo install -m 644 "$backup_config" "$nginx_config"
  else
    sudo rm -f "$nginx_config"
  fi
  "${compose[@]}" stop "${target_services[@]}"
  echo "Nginx validation failed; keeping $active_color active" >&2
  exit 1
fi

write_status deploying switch-traffic
sudo systemctl reload nginx
printf '%s\n' "$target_color" > "$ACTIVE_COLOR_FILE"
printf '%s\n' "$IMAGE_TAG" > "$ACTIVE_IMAGE_FILE"

if [[ "$active_color" != "none" && "$active_color" != "$target_color" ]]; then
  "${compose[@]}" stop "backend-$active_color" "frontend-$active_color" || true
fi

active_color="$target_color"
write_status success complete
trap - ERR
echo "Blue-green switch complete: $previous_color -> $target_color"
