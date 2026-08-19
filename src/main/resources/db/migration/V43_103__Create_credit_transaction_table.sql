do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_transaction_type') then
            create type credit_transaction_type as enum ('SUBSCRIPTION_GRANT', 'PURCHASE',
                'PURCHASE_REFUND', 'ADJUSTMENT', 'CONSUMPTION', 'CONSUMPTION_REVERSAL', 'EXPIRATION');
        end if;
    end
$$;

do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_transaction_movement_type') then
            create type credit_transaction_movement_type as enum ('CREDIT', 'DEBIT');
        end if;
    end
$$;

create table if not exists credit_transaction
(
    id                  varchar primary key                    default uuid_generate_v4(),
    user_id             varchar                     not null,
    type                credit_transaction_type            not null,
    movement_type       credit_transaction_movement_type   not null,
    credits             bigint                             not null,
    expiration_datetime timestamp without time zone,
    creation_datetime   timestamp without time zone            default current_timestamp,
    foreign key (user_id) references "user" (id)
);

create index if not exists credit_transaction_user_id_idx on credit_transaction (user_id);
