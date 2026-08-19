do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_adjustment_reason') then
            create type credit_adjustment_reason as enum ('COMMERCIAL_GESTURE',
                'INCIDENT_COMPENSATION', 'MIGRATION', 'CORRECTION');
        end if;
    end
$$;

alter table if exists credit_transaction
    add column if not exists label                   varchar,
    add column if not exists credit_purchase_id      varchar,
    add column if not exists adjustment_reason       credit_adjustment_reason,
    add column if not exists reversed_transaction_id varchar;
