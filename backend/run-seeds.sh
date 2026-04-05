#!/usr/bin/env bash
set -euo pipefail

# Ejecuta schema + seeds manualmente con psql en orden de dependencias.
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-EasySchedule}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
WITH_SCHEMA="${WITH_SCHEMA:-true}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCES_DIR="$SCRIPT_DIR/src/main/resources/db"

if ! command -v psql >/dev/null 2>&1; then
  echo "Error: psql no esta instalado o no esta en PATH."
  exit 1
fi

PSQL_BASE=(
  psql
  -v ON_ERROR_STOP=1
  -h "$DB_HOST"
  -p "$DB_PORT"
  -U "$DB_USER"
  -d "$DB_NAME"
)

export PGPASSWORD="$DB_PASSWORD"

echo "Ejecutando seeds en DB '$DB_NAME' ($DB_HOST:$DB_PORT) con usuario '$DB_USER'..."

if [[ "$WITH_SCHEMA" == "true" ]]; then
  "${PSQL_BASE[@]}" -f "$RESOURCES_DIR/schema.sql"
fi

"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_universidades.sql"
"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_carreras.sql"
"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_mallas.sql"
"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_materias.sql"
"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_malla_materia.sql"
"${PSQL_BASE[@]}" -f "$RESOURCES_DIR/seed/seed_prerequisitos.sql"

echo "Seeds ejecutados correctamente."
