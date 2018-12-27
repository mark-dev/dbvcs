do
\$\$
declare
	rec record;
	to_user_name varchar(40):='${dbuser}';
begin
        --Схемы
	for rec in(select 'alter schema '||nspname||' owner to '||to_user_name||';' as query from pg_namespace)
	loop
		execute rec.query;
	end loop;
        --Таблицы
	for rec in(SELECT 'ALTER TABLE '|| schemaname || '."' || tablename ||'" OWNER TO '||to_user_name||';' as query FROM pg_tables WHERE NOT schemaname IN ('pg_catalog', 'information_schema'))
	loop
		execute rec.query;
	end loop;
        --Последовательности
	for rec in(SELECT 'ALTER SEQUENCE '|| sequence_schema || '."' || sequence_name ||'" OWNER TO '||to_user_name||';' as query FROM information_schema.sequences WHERE NOT sequence_schema IN ('pg_catalog', 'information_schema'))
	loop
		execute rec.query;
	end loop;
        --Представления
	for rec in(SELECT 'ALTER VIEW '|| table_schema || '."' || table_name ||'" OWNER TO '||to_user_name||';' as query FROM information_schema.views WHERE NOT table_schema IN ('pg_catalog', 'information_schema'))
	loop
		execute rec.query;
	end loop;
        --Материализованные представления
	for rec in(SELECT 'ALTER TABLE '|| oid::regclass::text ||' OWNER TO '||to_user_name||';' as query FROM pg_class WHERE relkind = 'm')
	loop
		execute rec.query;
	end loop;
        --Функции
	for rec in(SELECT 'ALTER FUNCTION '||n.nspname||'.'||p.proname||'('||pg_catalog.pg_get_function_identity_arguments(p.oid)||') OWNER TO '||to_user_name||';' as query
                	FROM pg_catalog.pg_proc p
		             JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
                        where n.nspname NOT IN ('pg_catalog', 'information_schema'))
	loop
		execute rec.query;
	end loop;
end\$\$;