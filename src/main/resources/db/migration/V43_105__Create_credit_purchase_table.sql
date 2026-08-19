do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_purchase_status') then
            create type credit_purchase_status as enum ('PENDING', 'COMPLETED', 'FAILED',
                'EXPIRED', 'REFUNDED');
        end if;
    end
$$;

do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_purchase_origin') then
            create type credit_purchase_origin as enum ('SELF_SERVICE', 'AUTO_RECHARGE');
        end if;
    end
$$;

create table if not exists credit_purchase
(
    id                                     varchar primary key         default uuid_generate_v4(),
    user_id                                varchar                not null,
    type                                   credit_purchase_type   not null,
    credit_pack_id                         varchar,
    quantity                               integer,
    credits                                bigint,
    credit_unit_price_in_cents_without_vat bigint,
    amount_in_cents_without_vat            bigint,
    amount_in_cents_with_vat               bigint,
    vat_percent                            bigint,
    status                                 credit_purchase_status not null,
    origin                                 credit_purchase_origin not null,
    redirection_url                        varchar,
    redirection_success_url                varchar,
    redirection_failure_url                varchar,
    credit_transaction_id                  varchar,
    invoice_id                             varchar,
    completion_datetime                    timestamp without time zone,
    credits_expiration_datetime            timestamp without time zone,
    creation_datetime                      timestamp without time zone default current_timestamp,
    foreign key (user_id) references "user" (id),
    foreign key (credit_pack_id) references credit_pack (id)
);

create index if not exists credit_purchase_user_id_idx on credit_purchase (user_id);
