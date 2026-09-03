do
$$
    begin
        if not exists(select from pg_type where typname = 'email_recipient_type') then
            create type email_recipient_type as enum ('INVOICE', 'API_NOTIFICATION', 'ACCOUNT_INFO');
        end if;
    end
$$;

create table if not exists "email_recipient"
(
    id                varchar primary key    default uuid_generate_v4(),
    id_account_holder varchar      not null,
    "type"            email_recipient_type not null,
    email             varchar      not null,
    updated_at        timestamp without time zone default current_timestamp,
    foreign key (id_account_holder) references "account_holder" (id)
);

create index if not exists email_recipient_id_account_holder_idx
    on "email_recipient" (id_account_holder);

create unique index if not exists email_recipient_holder_type_email_unique_idx
    on "email_recipient" (id_account_holder, "type", email);
