do
$$
begin
        if not exists(select 1
                      from pg_enum
                      where enumlabel = 'SUBSCRIPTION_INVOICE_EXPORTER'
                        and enumtypid = (select oid from pg_type where typname = 'user_role')) then
alter type user_role add value 'SUBSCRIPTION_INVOICE_EXPORTER';
end if;
end
$$;
