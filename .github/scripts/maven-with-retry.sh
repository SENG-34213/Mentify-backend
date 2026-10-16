#!/usr/bin/env bash
set -uo pipefail

max_attempts=3
attempt=1

while (( attempt <= max_attempts )); do
  log_file="$(mktemp)"
  echo "Maven attempt ${attempt}/${max_attempts}"

  set +e
  mvn -U "$@" 2>&1 | tee "$log_file"
  status=${PIPESTATUS[0]}
  set -e

  if (( status == 0 )); then
    rm -f "$log_file"
    exit 0
  fi

  if (( attempt >= max_attempts )) || ! grep -Eiq 'HTTP Status: 5[0-9]{2}|status code: 5[0-9]{2}' "$log_file"; then
    rm -f "$log_file"
    exit "$status"
  fi

  rm -f "$log_file"
  delay_seconds=$((attempt * 10))
  echo "Maven Central returned a server error; retrying in ${delay_seconds}s."
  sleep "$delay_seconds"
  ((attempt += 1))
done
