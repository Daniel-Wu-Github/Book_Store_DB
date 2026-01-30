#!/usr/bin/env bash
set -euo pipefail

# run_react.sh - start backend and React/Vite frontend for development
# Usage: ./run_react.sh
# Requires: project root (where this script lives) and optional .env file

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# Load environment variables from .env (if present) and export them,
# but DO NOT override values already set in the environment.
if [ -f .env ]; then
  echo "Loading .env"
  for __v in BOOK_DB_HOST BOOK_DB_PORT BOOK_DB_NAME BOOK_DB_USER BOOK_DB_PASS APP_SEED_ENABLED USE_TMUX; do
    eval "__PREV_${__v}='${!__v-__UNSET__}'"
  done

  set -a
  # shellcheck disable=SC1091
  source .env
  set +a

  for __v in BOOK_DB_HOST BOOK_DB_PORT BOOK_DB_NAME BOOK_DB_USER BOOK_DB_PASS APP_SEED_ENABLED USE_TMUX; do
    eval "__prev=\$__PREV_${__v}"
    if [ "${__prev}" != "__UNSET__" ]; then
      eval "export ${__v}=\"${__prev}\""
    fi
  done
  unset __v __prev __PREV_BOOK_DB_HOST __PREV_BOOK_DB_PORT __PREV_BOOK_DB_NAME __PREV_BOOK_DB_USER __PREV_BOOK_DB_PASS __PREV_APP_SEED_ENABLED __PREV_USE_TMUX || true
fi

USE_TMUX=${USE_TMUX:-false}

LOG_DIR="${ROOT_DIR}/run-logs"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
BACKEND_PID="$LOG_DIR/backend.pid"
FRONTEND_LOG="$LOG_DIR/frontend-react.log"
FRONTEND_PID="$LOG_DIR/frontend-react.pid"

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

# Preflight: verify MySQL connectivity
DB_HOST=${BOOK_DB_HOST:-127.0.0.1}
DB_PORT=${BOOK_DB_PORT:-3306}
DB_NAME=${BOOK_DB_NAME:-bookstore}
DB_USER=${BOOK_DB_USER:-book_user}
DB_PASS=${BOOK_DB_PASS:-}

echo "Checking MySQL reachability at ${DB_HOST}:${DB_PORT} as ${DB_USER}..."
if ! (timeout 3 bash -lc "</dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null); then
  echo "Error: cannot open TCP connection to ${DB_HOST}:${DB_PORT}. Is MySQL running?" >&2
  exit 1
fi

if command -v mysql >/dev/null 2>&1; then
  if ! mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASS}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "Error: MySQL auth failed for '${DB_USER}'@'${DB_HOST}'." >&2
    exit 1
  else
    echo "MySQL connectivity OK."
  fi
else
  echo "Warning: mysql client not found; skipping SQL-level auth check."
fi

# Check if frontend/node_modules exists
if [ -d "frontend" ]; then
  if [ ! -d "frontend/node_modules" ]; then
    echo "Installing frontend dependencies..."
    (cd frontend && npm install)
  fi
else
  echo "Error: frontend/ directory not found." >&2
  exit 1
fi

# If user requested tmux and it's available
if [ "${USE_TMUX}" = "true" ] && command -v tmux >/dev/null 2>&1; then
  echo "Launching backend and React frontend inside tmux session 'bookstore-react'"
  tmux new-session -d -s bookstore-react -n backend "bash -lc 'mvn spring-boot:run -Dspring-boot.run.arguments=\"--app.seed.enabled=${APP_SEED_ENABLED:-false}\" > \"$BACKEND_LOG\" 2>&1'"
  tmux new-window -t bookstore-react -n frontend "bash -lc 'cd frontend && npm run dev > \"$FRONTEND_LOG\" 2>&1'"
  tmux new-window -t bookstore-react -n logs "bash -lc 'tail -f \"$BACKEND_LOG\" \"$FRONTEND_LOG\"'"
  echo "Attach with: tmux attach-session -t bookstore-react"
  echo "tmux-session-bookstore-react" > "$BACKEND_PID"
  echo "tmux-session-bookstore-react" > "$FRONTEND_PID"
else
  # Start backend in background
  ( mvn -q spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=${APP_SEED_ENABLED:-false}" > "$BACKEND_LOG" 2>&1 & echo $! > "$BACKEND_PID" )
fi

# Wait for backend to be ready (port 8080) up to 90s
echo "Waiting for backend port 8080 to become ready..."
for i in {1..90}; do
  if (echo >/dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; then
    echo "Backend is listening on 8080."
    break
  fi
  sleep 1
  if [ "$i" -eq 90 ]; then
    echo "Warning: backend did not open port 8080 within 90s; continuing."
  fi
done

# Start React/Vite frontend
echo "Starting React frontend (Vite dev server)... logs -> $FRONTEND_LOG"
( cd frontend && npm run dev > "$FRONTEND_LOG" 2>&1 & echo $! > "$FRONTEND_PID" )

# Wait for Vite to be ready (port 5173)
echo "Waiting for Vite dev server on port 5173..."
for i in {1..30}; do
  if (echo >/dev/tcp/127.0.0.1/5173) >/dev/null 2>&1; then
    echo "Vite dev server is listening on 5173."
    break
  fi
  sleep 1
  if [ "$i" -eq 30 ]; then
    echo "Warning: Vite did not open port 5173 within 30s."
  fi
done

echo ""
echo "=========================================="
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:5173"
echo "=========================================="
echo ""
echo "Backend PID: $(cat "$BACKEND_PID" 2>/dev/null || echo 'N/A')"
echo "Frontend PID: $(cat "$FRONTEND_PID" 2>/dev/null || echo 'N/A')"
echo ""
echo "Tailing logs (press Ctrl-C to stop and cleanup)"
tail -n +1 -f "$BACKEND_LOG" "$FRONTEND_LOG" || tail -n +1 -f "$BACKEND_LOG" || true
