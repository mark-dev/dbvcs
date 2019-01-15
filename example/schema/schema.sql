create table if not exists test_table
(
	id serial not null
		constraint test_table_pk
			primary key,
	name text default 'not defined'::text not null
);