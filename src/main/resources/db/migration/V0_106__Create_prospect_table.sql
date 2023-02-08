do
$$
    begin
        if not exists(select from pg_type where typname = 'prospect_status') then
            create type prospect_status as enum ('TO_CONTACT', 'CONTACTED', 'CONVERTED');
        end if;
    end
$$;

create table if not exists "prospect"
(
    id                varchar
        constraint prospect_pk primary key not null default uuid_generate_v4(),
    name              varchar              not null,
    email             varchar              not null,
    phone             varchar              not null,
    location          varchar              not null,
    status            prospect_status               default 'TO_CONTACT',
    id_account_holder varchar not null,
    constraint fk_prospect_account_holder foreign key (id_account_holder) references
        account_holder (id)
);
create index if not exists "prospect_account_holder_index" on "prospect"("id_account_holder");