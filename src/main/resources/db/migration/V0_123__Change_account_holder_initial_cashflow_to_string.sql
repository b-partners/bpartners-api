alter table account_holder
    alter column initial_cashflow type varchar using '0/1';