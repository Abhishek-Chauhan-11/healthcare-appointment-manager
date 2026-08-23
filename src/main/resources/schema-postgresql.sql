-- Convert Hibernate/JPA OID-backed String LOBs created by earlier deployments
-- into PostgreSQL text columns. Each conversion is conditional and idempotent.
DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('appointments', 'symptoms'),
            ('appointments', 'pre_visit_summary'),
            ('appointments', 'clinical_notes'),
            ('appointments', 'prescription'),
            ('appointments', 'post_visit_summary'),
            ('appointments', 'follow_up_instructions'),
            ('notification_jobs', 'body'),
            ('notification_jobs', 'last_error'),
            ('medication_reminders', 'medication_text'),
            ('google_calendar_tokens', 'access_token'),
            ('google_calendar_tokens', 'refresh_token')
        ) AS columns_to_convert(table_name, column_name)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = item.table_name
              AND column_name = item.column_name
              AND udt_name = 'oid'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE text USING CASE WHEN %I IS NULL THEN NULL ELSE convert_from(lo_get(%I), ''UTF8'') END',
                item.table_name,
                item.column_name,
                item.column_name,
                item.column_name
            );
        END IF;
    END LOOP;
END $$;
^^^ END OF SCRIPT ^^^
