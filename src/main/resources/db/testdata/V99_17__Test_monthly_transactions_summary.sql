insert into "monthly_transactions_summary"
(id, "year", "month", income, outcome, cash_flow, id_user)
values ('monthly_transactions_summary1_id', date_part('year', current_date), 0, '1356000/1',
        '1050/1',
        '1354950/1',
        'joe_doe_id'),
       ('monthly_transactions_summary2_id', date_part('year', current_date), 11, '0/1', '0/1',
        '1354950/1',
        'joe_doe_id');