#!/usr/bin/env bash
set -euo pipefail

create_database_if_missing() {
  local database_name="$1"

  if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$database_name'" | grep -q 1; then
    echo "Database '$database_name' already exists"
  else
    echo "Creating database '$database_name'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE \"$database_name\""
  fi
}

create_database_if_missing "${USER_DB_NAME:-user_db}"
create_database_if_missing "${COURSE_DB_NAME:-course_db}"
create_database_if_missing "${ENTROLLMENT_DB_NAME:-entrollment_db}"
create_database_if_missing "${ASSIGNMENT_DB_NAME:-assignment_db}"
create_database_if_missing "${COMMUNICATION_DB_NAME:-communication_db}"
create_database_if_missing "${ATTENDANCE_DB_NAME:-attendance_db}"
create_database_if_missing "${AI_DB_NAME:-ai_db}"
