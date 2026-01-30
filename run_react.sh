#!/usr/bin/env bash
set -euo pipefail

# run_react.sh - start backend and React/Vite frontend for development
# Usage: ./run_react.sh
# Requires: project root (where this script lives) and optional .env file

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# ─────────────────────────────────────────────────────────────────────────────
# Load environment variables from .env (if present) without overriding existing env vars
# ─────────────────────────────────────────────────────────────────────────────
load_env_file() {
  if [ -f .env ]; then
    echo "Loading .env..."
    while IFS= read -r line || [ -n "$line" ]; do
      # Skip empty lines, whitespace-only lines, and comments
      [[ -z "$line" || "$line" =~ ^[[:space:]]*$ || "$line" =~ ^[[:space:]]*# ]] && continue
      
      # Parse key=value (skip lines without =)
      [[ "$line" != *"="* ]] && continue
      
      # Extract key and value
      key="${line%%=*}"
      value="${line#*=}"
      
      # Remove leading/trailing whitespace from key
      key=$(echo "$key" | tr -d '[:space:]')
      
      # Skip if key is empty or not a valid variable name
      [[ -z "$key" || ! "$key" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] && continue
      
      # Skip if variable is already set in environment
      if [ -z "${!key+x}" ]; then
        # Remove surrounding quotes from value if present
        value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
        export "$key=$value"
      fi
    done < .env
  fi
}

load_env_file

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────
USE_TMUX=${USE_TMUX:-false}
BACKEND_PORT=8080
FRONTEND_PORTS=(5173 5174 5175 5176 5177 5178)

LOG_DIR="${ROOT_DIR}/run-logs"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_LOG="$LOG_DIR/frontend-react.log"
FRONTEND_PID_FILE="$LOG_DIR/frontend-react.pid"

# Database config (with defaults)
DB_HOST=${BOOK_DB_HOST:-127.0.0.1}
DB_PORT=${BOOK_DB_PORT:-3306}
DB_NAME=${BOOK_DB_NAME:-bookstore}
DB_USER=${BOOK_DB_USER:-book_user}
DB_PASS=${BOOK_DB_PASS:-}
APP_SEED_ENABLED=${APP_SEED_ENABLED:-false}

# ─────────────────────────────────────────────────────────────────────────────
# Utility functions
# ─────────────────────────────────────────────────────────────────────────────

# Kill process on a given port
kill_port() {
  local port=$1
  local pids
  
  # Try multiple methods to find processes on the port
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -ti ":$port" 2>/dev/null || true)
  elif command -v ss >/dev/null 2>&1; then
    pids=$(ss -tlnp "sport = :$port" 2>/dev/null | grep -oP 'pid=\K\d+' || true)
  elif command -v netstat >/dev/null 2>&1; then
    pids=$(netstat -tlnp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1 || true)
  fi
  
  if [ -n "$pids" ]; then
    echo "Killing existing process(es) on port $port: $pids"
    for pid in $pids; do
      kill -9 "$pid" 2>/dev/null || true
    done
    sleep 1
  fi
}

# Kill process by PID file
kill_pid_file() {
  local pid_file=$1
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file" 2>/dev/null)
    if [ -n "$pid" ] && [ "$pid" != "tmux-session-bookstore-react" ]; then
      # Kill the process and its children
      if kill -0 "$pid" 2>/dev/null; then
        echo "Killing process $pid from $pid_file"
        # Kill process group if possible
        kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
        # Also kill any child processes
        pkill -P "$pid" 2>/dev/null || true
      fi
    elif [ "$pid" = "tmux-session-bookstore-react" ]; then
      tmux kill-session -t bookstore-react 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
}

# Check if port is in use
port_in_use() {
  local port=$1
  if command -v lsof >/dev/null 2>&1; then
    lsof -i ":$port" >/dev/null 2>&1
  elif command -v ss >/dev/null 2>&1; then
    ss -tln "sport = :$port" 2>/dev/null | grep -q LISTEN
  elif command -v netstat >/dev/null 2>&1; then
    netstat -tln 2>/dev/null | grep -q ":$port "
  else
    # Fallback: try to connect
    (echo >/dev/tcp/127.0.0.1/"$port") 2>/dev/null
  fi
}

# Wait for a port to become available
wait_for_port() {
  local port=$1
  local timeout=${2:-90}
  local start_time=$SECONDS
  
  echo "Waiting for port $port to become ready (timeout: ${timeout}s)..."
  while ! port_in_use "$port"; do
    if [ $((SECONDS - start_time)) -ge "$timeout" ]; then
      echo "Warning: Port $port did not become ready within ${timeout}s"
      return 1
    fi
    sleep 1
  done
  echo "Port $port is now listening."
  return 0
}

# ─────────────────────────────────────────────────────────────────────────────
# Cleanup function (called on EXIT)
# ─────────────────────────────────────────────────────────────────────────────
cleanup() {
  echo ""
  echo "Stopping application processes..."
  
  kill_pid_file "$FRONTEND_PID_FILE"
  kill_pid_file "$BACKEND_PID_FILE"
  
  # Also kill any processes still on our ports
  kill_port $BACKEND_PORT
  for port in "${FRONTEND_PORTS[@]}"; do
    kill_port "$port"
  done
  
  echo "Done. Logs are in $LOG_DIR"
}

trap cleanup EXIT

# ─────────────────────────────────────────────────────────────────────────────
# Pre-flight: Kill any existing processes on our ports
# ─────────────────────────────────────────────────────────────────────────────
echo "Cleaning up any existing processes..."

# Kill old processes from PID files first
kill_pid_file "$BACKEND_PID_FILE"
kill_pid_file "$FRONTEND_PID_FILE"

# Then ensure ports are free
kill_port $BACKEND_PORT
for port in "${FRONTEND_PORTS[@]}"; do
  if port_in_use "$port"; then
    kill_port "$port"
  fi
done

# ─────────────────────────────────────────────────────────────────────────────
# Verify MySQL connectivity
# ─────────────────────────────────────────────────────────────────────────────
echo "Checking MySQL reachability at ${DB_HOST}:${DB_PORT} as ${DB_USER}..."

# Check TCP connectivity
mysql_reachable=false
if command -v nc >/dev/null 2>&1; then
  nc -z -w3 "$DB_HOST" "$DB_PORT" 2>/dev/null && mysql_reachable=true
elif command -v timeout >/dev/null 2>&1; then
  timeout 3 bash -c "</dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null && mysql_reachable=true
else
  # Last resort: just try to connect
  (echo >/dev/tcp/"${DB_HOST}"/"${DB_PORT}") 2>/dev/null && mysql_reachable=true
fi

if [ "$mysql_reachable" = false ]; then
  echo "Error: Cannot connect to MySQL at ${DB_HOST}:${DB_PORT}. Is MySQL running?" >&2
  exit 1
fi

# Verify MySQL authentication if client is available
if command -v mysql >/dev/null 2>&1; then
  MYSQL_AUTH_ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --protocol=TCP)
  if [ -n "$DB_PASS" ]; then
    MYSQL_AUTH_ARGS+=(-p"$DB_PASS")
  fi
  
  if ! mysql "${MYSQL_AUTH_ARGS[@]}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "Error: MySQL authentication failed for '${DB_USER}'@'${DB_HOST}'." >&2
    exit 1
  fi
  echo "MySQL connectivity OK."
else
  echo "Warning: mysql client not found; skipping SQL-level auth check."
fi

# ─────────────────────────────────────────────────────────────────────────────
# Ensure frontend dependencies are installed
# ─────────────────────────────────────────────────────────────────────────────
if [ ! -d "frontend" ]; then
  echo "Error: frontend/ directory not found." >&2
  exit 1
fi

if [ ! -d "frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  (cd frontend && npm install)
fi

# ─────────────────────────────────────────────────────────────────────────────
# Start backend (Spring Boot)
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "Starting backend (Spring Boot)... logs -> $BACKEND_LOG"

# Clear old log
: > "$BACKEND_LOG"

# Export database environment variables for Maven
export BOOK_DB_HOST="$DB_HOST"
export BOOK_DB_PORT="$DB_PORT"
export BOOK_DB_NAME="$DB_NAME"
export BOOK_DB_USER="$DB_USER"
export BOOK_DB_PASS="$DB_PASS"
export APP_SEED_ENABLED

if [ "${USE_TMUX}" = "true" ] && command -v tmux >/dev/null 2>&1; then
  echo "Launching in tmux session 'bookstore-react'..."
  tmux kill-session -t bookstore-react 2>/dev/null || true
  tmux new-session -d -s bookstore-react -n backend \
    "cd '$ROOT_DIR' && mvn spring-boot:run -Dspring-boot.run.arguments='--app.seed.enabled=${APP_SEED_ENABLED}' 2>&1 | tee '$BACKEND_LOG'"
  tmux new-window -t bookstore-react -n frontend \
    "cd '$ROOT_DIR/frontend' && npm run dev 2>&1 | tee '$FRONTEND_LOG'"
  tmux new-window -t bookstore-react -n logs \
    "tail -f '$BACKEND_LOG' '$FRONTEND_LOG'"
  
  echo "tmux-session-bookstore-react" > "$BACKEND_PID_FILE"
  echo "tmux-session-bookstore-react" > "$FRONTEND_PID_FILE"
  echo "Attach with: tmux attach-session -t bookstore-react"
else
  # Start backend in background and capture the actual PID
  mvn spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=${APP_SEED_ENABLED}" \
    >> "$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!
  echo "$BACKEND_PID" > "$BACKEND_PID_FILE"
  echo "Backend PID: $BACKEND_PID"
fi

# Wait for backend to be ready
if ! wait_for_port $BACKEND_PORT 120; then
  echo "Backend failed to start. Check logs: $BACKEND_LOG"
  echo "Last 30 lines of backend log:"
  tail -30 "$BACKEND_LOG"
  exit 1
fi

# ─────────────────────────────────────────────────────────────────────────────
# Start frontend (Vite dev server)
# ─────────────────────────────────────────────────────────────────────────────
if [ "${USE_TMUX}" != "true" ]; then
  echo ""
  echo "Starting React frontend (Vite dev server)... logs -> $FRONTEND_LOG"
  
  # Clear old log
  : > "$FRONTEND_LOG"
  
  # Start frontend in background
  (cd frontend && npm run dev >> "$FRONTEND_LOG" 2>&1) &
  FRONTEND_PID=$!
  echo "$FRONTEND_PID" > "$FRONTEND_PID_FILE"
  echo "Frontend PID: $FRONTEND_PID"
  
  # Wait for Vite to announce its URL
  FRONTEND_URL=""
  for i in {1..30}; do
    if grep -q "Local:" "$FRONTEND_LOG" 2>/dev/null; then
      FRONTEND_URL=$(grep -m1 "Local:" "$FRONTEND_LOG" | grep -oP 'http://[^\s]+' | head -1 | sed 's#/$##') || true
      if [ -n "$FRONTEND_URL" ]; then
        echo "Vite dev server ready: $FRONTEND_URL"
        break
      fi
    fi
    
    # Fallback: check if any Vite port is in use
    for port in "${FRONTEND_PORTS[@]}"; do
      if port_in_use "$port"; then
        FRONTEND_URL="http://localhost:${port}"
        echo "Detected Vite dev server on $FRONTEND_URL"
        break 2
      fi
    done
    
    sleep 1
    if [ "$i" -eq 30 ]; then
      echo "Warning: Vite did not announce a listening URL within 30s."
      FRONTEND_URL="http://localhost:5173"
    fi
  done
fi

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "  Backend:  http://localhost:${BACKEND_PORT}"
echo "  Frontend: ${FRONTEND_URL:-http://localhost:5173}"
echo "=========================================="
echo ""
echo "Backend PID:  $(cat "$BACKEND_PID_FILE" 2>/dev/null || echo 'N/A')"
echo "Frontend PID: $(cat "$FRONTEND_PID_FILE" 2>/dev/null || echo 'N/A')"
echo ""
echo "Press Ctrl-C to stop both servers"
echo ""

# Tail logs (keep script running)
tail -n +1 -f "$BACKEND_LOG" "$FRONTEND_LOG" 2>/dev/null || tail -n +1 -f "$BACKEND_LOG" 2>/dev/null || true
