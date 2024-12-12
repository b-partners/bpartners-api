do
$$
begin
        if not exists(select 1 from pg_enum where enumlabel = 'ADMIN_ROLE' and enumtypid = (select oid from pg_type where typname = 'user_role')) then
alter type user_role add value 'ADMIN_ROLE';
end if;
end
$$;
