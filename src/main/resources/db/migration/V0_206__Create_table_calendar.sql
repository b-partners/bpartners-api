do
$$
    begin
        if not exists(select from pg_type where typname = 'calendar_permission') then
            create type calendar_permission
            as enum ('OWNER', 'WRITER','READER');
        end if;
    end
$$;

create table if not exists "calendar"
(
    id         varchar
        constraint calendar_pk primary key not null default uuid_generate_v4
        (),
    ete_id     varchar,
    id_user    varchar,
    summary    varchar,
    permission calendar_permission,
    created_at timestamp                                         default current_timestamp,
    constraint calendar_user_fk foreign key (id_user) references "user" (id)
);