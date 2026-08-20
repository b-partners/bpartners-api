alter table if exists credit_transaction
    add column if not exists subscription_product_id varchar,
    add column if not exists grant_period_start      date;

do
$$
    begin
        if not exists(select
                      from pg_constraint
                      where conname = 'credit_transaction_subscription_product_id_fk') then
            alter table credit_transaction
                add constraint credit_transaction_subscription_product_id_fk
                    foreign key (subscription_product_id) references subscription_product (id);
        end if;
    end
$$;

with latest_active_plan as (
    select distinct on (user_id) user_id, subscription_product_id
    from user_subscription_product
    where subscription_end_datetime is null
       or subscription_end_datetime > current_timestamp
    order by user_id, subscription_start_datetime desc
),
     first_grant_of_period as (
         select distinct on (user_id, paris_period_start) id,
                                                          user_id,
                                                          paris_period_start::date as period_start
         from (select id,
                      user_id,
                      creation_datetime,
                      date_trunc('month',
                                 creation_datetime at time zone 'UTC' at time zone 'Europe/Paris')
                          as paris_period_start
               from credit_transaction
               where type = 'SUBSCRIPTION_GRANT') as paris_dated_grant
         order by user_id, paris_period_start, creation_datetime
     )
update credit_transaction ct
set subscription_product_id = latest_active_plan.subscription_product_id,
    grant_period_start      = first_grant_of_period.period_start
from first_grant_of_period
         join latest_active_plan on latest_active_plan.user_id = first_grant_of_period.user_id
where ct.id = first_grant_of_period.id;

create unique index if not exists credit_transaction_subscription_grant_period_unique_idx
    on credit_transaction (user_id, subscription_product_id, grant_period_start)
    where type = 'SUBSCRIPTION_GRANT';
