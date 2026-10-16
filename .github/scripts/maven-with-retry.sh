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

  retryable_resolution_failure=false
  if grep -Eiq \
    'HTTP Status: 5[0-9]{2}|status code: 5[0-9]{2}|Could not find artifact .* in central|Could not transfer artifact .* from/to central|was not found in .* during a previous attempt' \
    "$log_file"; then
    retryable_resolution_failure=true
  fi

  if (( attempt >= max_attempts )) || [[ "$retryable_resolution_failure" != true ]]; then
    rm -f "$log_file"
    exit "$status"
  fi

  # Maven can cache failed or incomplete Central lookups in the shared Actions cache.
  if [[ -d "$HOME/.m2/repository" ]]; then
    find "$HOME/.m2/repository" -type f -name '*.lastUpdated' -delete
  fi
  rm -f "$log_file"
  delay_seconds=$((attempt * 10))
  echo "Maven Central dependency resolution failed; retrying in ${delay_seconds}s."
  sleep "$delay_seconds"
  ((attempt += 1))
done
