do
$$
    begin
        if not exists(select from pg_type where typname = 'email_status') then
            create type email_status
            as enum ('DRAFT', 'SENT');
        end if;
    end
$$;

create table if not exists "email"
(
    id         varchar
        constraint email_pk primary key default uuid_generate_v4(),
    id_user    varchar,
    recipients varchar,
    object     varchar,
    body       varchar,
    status     email_status,
    constraint fk_email_user foreign key (id_user) references "user" (id)
)