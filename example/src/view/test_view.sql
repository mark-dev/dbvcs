create or replace view test_view as
SELECT test_table.id,
       (test_table.name || 'aaaa'::text)
FROM test_table;