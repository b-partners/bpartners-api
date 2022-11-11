create table if not exists "monthly_transactions_summary"
(
    id         varchar
    constraint monthly_transactions_summary_pk primary key default uuid_generate_v4(),
    "year"      integer,
    "month"     integer check ("month" >= 0 and "month" <= 11),
    income      varchar,
    outcome     varchar,
    cash_flow   varchar,
    updated_at  timestamp default current_timestamp,
    constraint year_month_unique unique("year","month")
);

alter function update_updated_at_invoice rename to update_updated_at;

create trigger update_monthly_transactions_summary_updated_at
    before update
    on
        "monthly_transactions_summary"
    for each row
    execute procedure update_updated_at();

