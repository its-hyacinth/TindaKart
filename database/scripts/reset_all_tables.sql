-- DANGER: This permanently deletes every table, view, sequence, and Flyway history
-- record in the current PostgreSQL database. Run only against a disposable local DB.
--
-- Example:
--   psql "$env:DATABASE_URL" -v ON_ERROR_STOP=1 -f database/scripts/reset_all_tables.sql
--
-- Flyway will recreate the schema from the migrations the next time the backend starts.

BEGIN;

DO $$
DECLARE
    database_object RECORD;
BEGIN
    FOR database_object IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', database_object.tablename);
    END LOOP;

    FOR database_object IN
        SELECT viewname
        FROM pg_views
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format('DROP VIEW IF EXISTS public.%I CASCADE', database_object.viewname);
    END LOOP;

    FOR database_object IN
        SELECT sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    LOOP
        EXECUTE format('DROP SEQUENCE IF EXISTS public.%I CASCADE', database_object.sequence_name);
    END LOOP;
END $$;

COMMIT;
