insert into "monthly_transactions_summary"
(id, "year", "month", income, outcome, cash_flow, id_account)
values ('monthly_transactions_summary1_id', date_part('year', current_date), 0, '1000000/1', '0/1',
        '1000000/1',
        'beed1765-5c16-472a-b3f4-5c376ce5db58'),
       ('monthly_transactions_summary2_id', date_part('year', current_date), 11, '0/1', '0/1',
        '1000000/1',
        'beed1765-5c16-472a-b3f4-5c376ce5db58');