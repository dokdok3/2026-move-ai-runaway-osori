#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-$HOME/hackathon-deploy}"

packages=(nginx openssl curl)
if ! command -v docker >/dev/null 2>&1; then
  packages+=(docker.io docker-compose-v2)
elif ! docker compose version >/dev/null 2>&1; then
  packages+=(docker-compose-v2)
fi

sudo apt-get update
sudo apt-get install -y "${packages[@]}"
sudo systemctl enable --now docker nginx
sudo usermod -aG docker "$USER"

mkdir -p "$DEPLOY_DIR/scripts" "$DEPLOY_DIR/deploy/nginx"
sudo install -d -o "$USER" -g "$USER" -m 755 /var/www/hackathon-deploy

certificate_dir="/etc/nginx/ssl"
certificate_file="$certificate_dir/osori-selfsigned.crt"
certificate_key="$certificate_dir/osori-selfsigned.key"
if ! sudo test -f "$certificate_file" || ! sudo test -f "$certificate_key"; then
  public_ip="$(curl --fail --silent https://checkip.amazonaws.com | tr -d '[:space:]')"
  sudo install -d -m 700 "$certificate_dir"
  sudo openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
    -keyout "$certificate_key" \
    -out "$certificate_file" \
    -subj "/CN=$public_ip" \
    -addext "subjectAltName=IP:$public_ip"
  sudo chmod 600 "$certificate_key"
  sudo chmod 644 "$certificate_file"
fi

if [[ ! -f "$DEPLOY_DIR/.env" ]]; then
  umask 077
  postgres_password="$(openssl rand -hex 24)"
  printf 'POSTGRES_PASSWORD=%s\n' "$postgres_password" > "$DEPLOY_DIR/.env"
fi

if [[ -L /etc/nginx/sites-enabled/default ]]; then
  sudo unlink /etc/nginx/sites-enabled/default
fi

echo "EC2 setup complete. Sign out and reconnect once to apply Docker group membership."
