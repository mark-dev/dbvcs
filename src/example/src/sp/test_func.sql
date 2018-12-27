create function test_func() returns SETOF bigint
	language plpgsql
as $$
BEGIN
	return query select count(*) FROM test_table;
END
$$;