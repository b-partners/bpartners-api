do
$$
    begin
        if not exists(select from pg_type where typname = 'identification_status') then
            create type identification_status
            as enum ('VALID_IDENTITY', 'PROCESSING','INVALID_IDENTITY',
                'INSUFFICIENT_DOCUMENT_QUALITY','UNINITIATED');
        end if;
    end
$$;

alter table "user"
    add column if not exists identification_status identification_status default 'VALID_IDENTITY';
alter table "user"
    add column if not exists first_name varchar;
alter table "user"
    add column if not exists last_name varchar;
alter table "user"
    add column if not exists id_verified boolean;