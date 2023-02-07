create table if not exists "payment_request"
(
    id               varchar
        constraint payment_request_pk primary key,
    session_id       varchar,
    id_invoice       varchar,
    constraint fk_invoice_payment foreign key (id_invoice) references invoice ("id"),
    account_id       varchar not null,
    payment_url      varchar,
    label            varchar,
    reference        varchar,
    amount           varchar,
    payer_name       varchar not null,
    payer_email      varchar not null,
    created_datetime timestamp without time zone default current_timestamp
);
create index "payment_request_session_id" on payment_request (session_id);
create index "payment_request_invoice" on payment_request (id_invoice);
create index "payment_request_account" on payment_request (account_id);