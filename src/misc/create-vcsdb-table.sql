create table if not exists dbvcs
(
	id serial not null
		constraint dbvcs_pk
			primary key,
	version integer not null,
	script_name text not null,
	ts timestamp default timezone('UTC'::text, now()) not null
);

create index if not exists dbvcs_version_uindex
	on dbvcs (version);