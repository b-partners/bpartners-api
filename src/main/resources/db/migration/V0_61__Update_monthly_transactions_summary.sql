alter table "monthly_transactions_summary"
drop
constraint if exists year_month_unique;
alter table "monthly_transactions_summary"
    add constraint account_year_month_unique unique (id_account, "year", "month");