do
$$
    begin
        if not exists(select from pg_type where typname = 'subscription_termination_status') then
            create type subscription_termination_status as enum
                ('PENDING', 'SUBSCRIPTION_ENDED', 'CREDIT_NOTE_ISSUED', 'SKIPPED', 'FAILED');
        end if;
    end
$$;

create table if not exists temp_subscription_termination
(
    id                             varchar primary key default uuid_generate_v4(),
    user_subscription_e2_id        varchar                         not null unique,
    subscription_end_datetime      timestamp without time zone     not null,
    current_period_end_date        date                            not null,
    amount_in_cents_with_vat       bigint                          not null,
    status                         subscription_termination_status not null default 'PENDING',
    user_id                        varchar,
    credit_note_invoice_id         varchar,
    error_message                  varchar,
    update_datetime                timestamp without time zone     not null default current_timestamp
);
