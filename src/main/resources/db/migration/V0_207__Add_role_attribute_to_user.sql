do
$$
    begin
        if not exists(select from pg_type where typname = 'user_role') then
            create type user_role
            as enum ('EVAL_PROSPECT');
        end if;
    end
$$;
alter table "user"
    add column if not exists roles user_role[] default '{}';