do
$$
begin
        if not exists(select from pg_type where typname = 'relaunch_type') then
create type "relaunch_type" as enum ('PROPOSAL','CONFIRMED');
end if;
end
$$;

create table if not exists "invoice_relaunch"
(
    id varchar
    constraint invoice_relaunch_pk primary key default uuid_generate_v4(),
    "type" relaunch_type not null,
    id_invoice varchar not null,
    is_user_relaunched boolean not null,
    creation_datetime timestamp with time zone not null,
    constraint invoice_relaunch_fk foreign key (id_invoice)
    references "invoice"(id)
);
