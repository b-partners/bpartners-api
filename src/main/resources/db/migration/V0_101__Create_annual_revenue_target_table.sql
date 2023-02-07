create table if not exists "annual_revenue_target"(
    id varchar
        constraint annual_revenue_pk primary key default uuid_generate_v4(),
    "year" int ,
    id_account_holder varchar,
    amount_target varchar,
    updated_at timestamp with time zone default current_timestamp,
    constraint annual_revenue_fk foreign key (id_account_holder)
        references "account_holder"(id)
)
