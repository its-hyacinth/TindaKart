# TindaKart database migrations

Database changes for the PWA/backend migration belong in this directory as ordered migration files.

Rules:

- Back up the existing PostgreSQL database before applying migrations.
- Never edit an already-applied migration; add a new migration instead.
- Keep the legacy Swing tables until the replacement workflows are verified.
- Do not store passwords or production data in this directory.
