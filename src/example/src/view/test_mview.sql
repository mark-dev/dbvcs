CREATE MATERIALIZED VIEW test_mview as
SELECT count(*) FROM test_view;