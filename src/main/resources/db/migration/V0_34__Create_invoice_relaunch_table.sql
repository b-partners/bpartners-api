create table if not exists "invoice_relaunch"
(
    id               varchar
        constraint relaunch_ok primary key default uuid_generate_v4(),
    account_id varchar not null,
    draft_relaunch   integer,
    unpaid_relaunch  integer,
    created_datetime timestamp with time zone
                                           default current_timestamp
);