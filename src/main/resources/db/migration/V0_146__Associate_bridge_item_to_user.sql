do
$$
    begin
        if not exists(select from pg_type where typname = 'bank_connection_status') then
            create type bank_connection_status as enum (
                'OK', 'NOT_SUPPORTED','VALIDATION_REQUIRED','UNKNOWN');
        end if;
    end
$$;

alter table "user"
    add column if not exists bridge_item_id            bigint,
    add column if not exists bank_connection_status    bank_connection_status,
    add column if not exists bridge_item_updated_at timestamp with time zone,
    add column if not exists bridge_item_last_refresh  timestamp with time zone;
