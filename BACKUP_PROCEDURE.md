# TindaKart database backup and recovery

Backups are local operational artifacts and are ignored by Git. The current local backup was created at:

- `backups/tindakart-local-latest.dump` — PostgreSQL custom-format archive
- `backups/tindakart-schema-latest.sql` — schema-only export

The archive was verified with `pg_restore --list` after creation and restored successfully into an isolated temporary database. The temporary database was removed afterward; no active database was dropped or reset.

## Create a backup

Run from the repository root after loading the same environment variables used by the backend:

```powershell
pg_dump --format=custom --file backups/tindakart-local-latest.dump `
  --host $env:DB_HOST --port $env:DB_PORT --username $env:DB_USER --dbname $env:DB_NAME
```

Keep `PGPASSWORD` or an equivalent password mechanism in the local environment only; never put it in this document or source control.

## Verify an archive

```powershell
pg_restore --list backups/tindakart-local-latest.dump
```

The command should list the archive metadata and table entries without connecting to or modifying a database.

## Restore safely

Restore to a newly created, isolated database first. Do not use `--clean`, `--create`, or a production database until the owner approves the target and a maintenance window.

```powershell
createdb --host $env:DB_HOST --port $env:DB_PORT --username $env:DB_USER tindakart_restore_check
pg_restore --exit-on-error --no-owner --host $env:DB_HOST --port $env:DB_PORT `
  --username $env:DB_USER --dbname tindakart_restore_check backups/tindakart-local-latest.dump
```

After validation, remove the temporary restore database using the database administrator's approved procedure. Keep the original database untouched during backup and restore verification.

## Current schema evidence

The schema-only export records the current columns, constraints, indexes, and relationships. The authoritative application schema remains the versioned Flyway migrations under `backend/src/main/resources/db/migration`.
