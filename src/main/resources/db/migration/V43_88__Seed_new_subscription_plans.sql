insert into "subscription_product" (id, name, features, type, price_in_cents, creation_datetime,
                                    plan_code, billing_type, free_usage_threshold,
                                    overage_unit_price_in_cents, trial_period_days)
values ('4219611e-7584-4636-a3c5-ba212600715b', 'À l''usage', '[]', 'MONTHLY', 0, current_timestamp,
        'USAGE', 'USAGE_BASED', 0, null, 0),
       ('89f1acdd-c3b9-4717-a21d-355b2021ad58', 'Essentiel V2', '[]', 'MONTHLY', 5880,
        current_timestamp, 'ESSENTIAL_V2', 'COMMITMENT', 10, 500, 0),
       ('c5f57306-a7b1-43f4-90fc-204ccd4c0ce2', 'Pro', '[]', 'MONTHLY', 11880, current_timestamp,
        'PRO', 'COMMITMENT', 25, 400, 0),
       ('37b9639e-d058-4222-8a2a-d78d5fe7b6b1', 'Expert', '[]', 'MONTHLY', 23880, current_timestamp,
        'EXPERT', 'COMMITMENT', 60, 300, 0);

update "subscription_product"
set metered_product_id =
        (select id
         from "subscription_product"
         where consumption_type_attached = 'ROOF_ANALYSIS'
         limit 1)
where id in ('4219611e-7584-4636-a3c5-ba212600715b',
             '89f1acdd-c3b9-4717-a21d-355b2021ad58',
             'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2',
             '37b9639e-d058-4222-8a2a-d78d5fe7b6b1');
