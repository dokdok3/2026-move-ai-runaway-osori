#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-$HOME/hackathon-deploy}"

sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 nginx openssl curl
sudo systemctl enable --now docker nginx
sudo usermod -aG docker "$USER"

mkdir -p "$DEPLOY_DIR/scripts" "$DEPLOY_DIR/deploy/nginx"

if [[ ! -f "$DEPLOY_DIR/.env" ]]; then
  umask 077
  postgres_password="$(openssl rand -hex 24)"
  printf 'POSTGRES_PASSWORD=%s\n' "$postgres_password" > "$DEPLOY_DIR/.env"
fi

if [[ -L /etc/nginx/sites-enabled/default ]]; then
  sudo unlink /etc/nginx/sites-enabled/default
fi

echo "EC2 setup complete. Sign out and reconnect once to apply Docker group membership."
