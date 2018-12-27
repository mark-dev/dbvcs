DO
  \$\$
    declare
      r                     record;
      declare target_schema text := '${schema}';
    begin
      for r in
        select format('%s.%s', nspname, c.oid::regclass::text) as fmt_str,relkind
        from pg_class c
               join pg_namespace n on n.oid = relnamespace
        where nspname = target_schema
          and relkind in ('v', 'm')
        loop
          IF r.relkind = 'v' THEN
            execute format('DROP VIEW %s CASCADE', r.fmt_str);
          ELSEIF r.relkind = 'm' THEN
          --TODO: Это не работает, почему-то пишет что вью не найден
           -- execute format('DROP MATERIALIZED VIEW %s CASCADE', r.fmt_str);
          END IF;
          --return next r;
        end loop;
    end
    \$\$;

DO
  \$\$
    declare
      r                     text;
      declare target_schema text := '${schema}';
    begin
      for r in select format('%s.%s(%s)',
                             nspname, proname, pg_get_function_identity_arguments(p.oid))
               from pg_proc p
                      join pg_namespace n on n.oid = pronamespace
               WHERE nspname = target_schema
        loop
          execute format('drop function %s', r);
        end loop;
    end
    \$\$;