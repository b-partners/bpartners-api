create table if not exists invoice_summary
(
    id              varchar
        constraint invoice_summary_pk primary key default uuid_generate_v4(),
    id_user      varchar,
    paid_amount     varchar,
    unpaid_amount   varchar,
    proposal_amount varchar,
    updated_at      timestamp with time zone,
    constraint invoice_summary_user_fk foreign key (id_user) references "user" (id)
);