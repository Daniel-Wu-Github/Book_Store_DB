#!/usr/bin/env bash
set -euo pipefail

# run.sh - start backend and JavaFX frontend for development
# Usage: ./run.sh
# Requires: project root (where this script lives) and optional .env file

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# Load environment variables from .env (if present) and export them,
# but DO NOT override values already set in the environment.
if [ -f .env ]; then
  echo "Loading .env"
  # Preserve any values the user already provided on the command line
  # e.g., APP_SEED_ENABLED=true ./run.sh
  for __v in BOOK_DB_HOST BOOK_DB_PORT BOOK_DB_NAME BOOK_DB_USER BOOK_DB_PASS APP_SEED_ENABLED USE_TMUX; do
    eval "__PREV_${__v}='${!__v-__UNSET__}'"
  done

  set -a
  # shellcheck disable=SC1091
  source .env
  set +a

  # Restore pre-set values so .env does not override them
  for __v in BOOK_DB_HOST BOOK_DB_PORT BOOK_DB_NAME BOOK_DB_USER BOOK_DB_PASS APP_SEED_ENABLED USE_TMUX; do
    eval "__prev=\$__PREV_${__v}"
    if [ "${__prev}" != "__UNSET__" ]; then
      eval "export ${__v}=\"${__prev}\""
    fi
  done
  unset __v __prev __PREV_BOOK_DB_HOST __PREV_BOOK_DB_PORT __PREV_BOOK_DB_NAME __PREV_BOOK_DB_USER __PREV_BOOK_DB_PASS __PREV_APP_SEED_ENABLED __PREV_USE_TMUX || true
fi

# If you want separate terminal panes/windows, set USE_TMUX=true in your .env
# or run the script with USE_TMUX=true ./run.sh
USE_TMUX=${USE_TMUX:-${USE_TMUX:-false}}

LOG_DIR="${ROOT_DIR}/run-logs"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
BACKEND_PID="$LOG_DIR/backend.pid"
FRONTEND_LOG="$LOG_DIR/frontend.log"
FRONTEND_PID="$LOG_DIR/frontend.pid"

cleanup() {
  echo "Stopping application processes..."
  if [ -f "$FRONTEND_PID" ]; then
    kill "$(cat "$FRONTEND_PID")" 2>/dev/null || true
    rm -f "$FRONTEND_PID"
  fi
  if [ -f "$BACKEND_PID" ]; then
    kill "$(cat "$BACKEND_PID")" 2>/dev/null || true
    rm -f "$BACKEND_PID"
  fi
  echo "Done. Logs are in $LOG_DIR"
}
trap cleanup EXIT

echo "Starting backend (Spring Boot)... logs -> $BACKEND_LOG"
# Preflight: verify MySQL connectivity with provided credentials; fail fast if not reachable
DB_HOST=${BOOK_DB_HOST:-127.0.0.1}
DB_PORT=${BOOK_DB_PORT:-3306}
DB_NAME=${BOOK_DB_NAME:-bookstore}
DB_USER=${BOOK_DB_USER:-book_user}
DB_PASS=${BOOK_DB_PASS:-}

echo "Checking MySQL reachability at ${DB_HOST}:${DB_PORT} as ${DB_USER}..."
# TCP reachability check (3s)
if ! (timeout 3 bash -lc "</dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null); then
  echo "Error: cannot open TCP connection to ${DB_HOST}:${DB_PORT}. Is MySQL running and accessible from WSL?" >&2
  echo "Tip: If MySQL runs on Windows, ensure it's listening on 127.0.0.1:3306 and firewall allows local connections." >&2
  exit 1
fi

if command -v mysql >/dev/null 2>&1; then
  # Force TCP so this preflight matches how the app connects via JDBC
  if ! mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASS}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "Error: MySQL auth failed for '${DB_USER}'@'${DB_HOST}'." >&2
    echo "Run these checks in MySQL as an admin (root):" >&2
    echo "  SHOW GRANTS FOR '${DB_USER}'@'localhost';" >&2
    echo "  SHOW GRANTS FOR '${DB_USER}'@'127.0.0.1';" >&2
    echo "If missing, create/grant user (adjust password and host as needed):" >&2
    echo "  CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';" >&2
    echo "  CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';" >&2
    echo "  GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';" >&2
    echo "  GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';" >&2
    echo "  FLUSH PRIVILEGES;" >&2
    echo "If the user already exists with a different password or plugin, reset it:" >&2
    echo "  ALTER USER '${DB_USER}'@'localhost' IDENTIFIED WITH caching_sha2_password BY '${DB_PASS}';" >&2
    echo "  ALTER USER '${DB_USER}'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '${DB_PASS}';" >&2
    exit 1
  else
    echo "MySQL connectivity OK."
  fi
else
  echo "Warning: mysql client not found; skipping SQL-level auth check."
fi

# If user requested tmux and it's available, create a tmux session with separate windows
if [ "${USE_TMUX}" = "true" ] && command -v tmux >/dev/null 2>&1; then
  echo "Launching backend and frontend inside tmux session 'bookstore'"
  # create session (detached) and run backend in window 0
  tmux new-session -d -s bookstore -n backend "bash -lc 'mvn spring-boot:run -Dspring-boot.run.arguments=\"--app.seed.enabled=${APP_SEED_ENABLED:-false}\" > \"$BACKEND_LOG\" 2>&1'"
  # create frontend window if module exists
  if [ -d "javafx_frontend" ]; then
    tmux new-window -t bookstore -n frontend "bash -lc 'cd javafx_frontend && mvn javafx:run > \"$FRONTEND_LOG\" 2>&1'"
  fi
  # create logs window to tail both logs
  tmux new-window -t bookstore -n logs "bash -lc 'tail -f \"$BACKEND_LOG\" \"$FRONTEND_LOG\"'"
  echo "Attach with: tmux attach-session -t bookstore"
  # write fake PIDs for compatibility (tmux session id)
  echo "tmux-session-bookstore" > "$BACKEND_PID"
  echo "tmux-session-bookstore" > "$FRONTEND_PID"
else
  # start backend in background
  ( mvn -q spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=${APP_SEED_ENABLED:-false}" > "$BACKEND_LOG" 2>&1 & echo $! > "$BACKEND_PID" )
fi

# Wait for backend to be ready (port 8080) up to 60s
echo "Waiting for backend port 8080 to become ready..."
for i in {1..60}; do
  if (echo >/dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; then
    echo "Backend is listening on 8080."
    break
  fi
  sleep 1
  if [ "$i" -eq 60 ]; then
    echo "Warning: backend did not open port 8080 within 60s; continuing."
  fi
done

if [ -d "javafx_frontend" ]; then
  echo "Starting JavaFX frontend... logs -> $FRONTEND_LOG"
  ( cd javafx_frontend && mvn -q javafx:run > "$FRONTEND_LOG" 2>&1 & echo $! > "$FRONTEND_PID" )
else
  echo "No javafx_frontend module found; skipping frontend start."
fi

echo "Backend PID: $(cat "$BACKEND_PID" 2>/dev/null || echo 'N/A')"
echo "Frontend PID: $(cat "$FRONTEND_PID" 2>/dev/null || echo 'N/A')"

echo "Tailing logs (press Ctrl-C to stop and cleanup)"
tail -n +1 -f "$BACKEND_LOG" "$FRONTEND_LOG" || tail -n +1 -f "$BACKEND_LOG" || true
