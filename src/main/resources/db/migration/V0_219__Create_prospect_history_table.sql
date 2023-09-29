create table if not exists "prospect_history"
(
    id varchar constraint prospect_history_pk primary key default uuid_generate_v4(),
    status prospect_status default 'TO_CONTACT',
    id_account_holder varchar,
    id_prospect varchar,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint prospect_history_account_holder_fk foreign key(id_account_holder) references "account_holder"(id),
    constraint prospect_history_prospect_fk foreign_key(id_prospect) references "prospect"(id)
);