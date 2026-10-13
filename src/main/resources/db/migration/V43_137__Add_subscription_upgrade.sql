alter table if exists "subscription_product"
    add column if not exists tier_rank integer;

update "subscription_product" sp
set tier_rank = ranked.position
from (select id,
             row_number() over (order by price_in_cents_without_vat asc, display_position asc nulls last, id) as position
      from "subscription_product"
      where billing_type is not null
        and deprecated = false) ranked
where sp.id = ranked.id
  and sp.tier_rank is null;

alter table if exists "user_subscription_commitment"
    add column if not exists expired_datetime timestamp without time zone;

do
$$
    begin
        if not exists (select 1 from pg_type where typname = 'subscription_upgrade_status') then
            create type subscription_upgrade_status as enum ('PENDING', 'PAID', 'INVOICED');
        end if;
    end
$$;

create table if not exists user_subscription_upgrade
(
    id                              varchar primary key default uuid_generate_v4(),
    user_id                         varchar                       not null,
    from_subscription_product_id    varchar                       not null,
    to_subscription_product_id      varchar                       not null,
    from_billing_interval           billing_interval              not null,
    to_billing_interval             billing_interval              not null,
    upgrade_datetime                timestamp without time zone   not null,
    old_coverage_end_datetime       timestamp without time zone   not null,
    new_commitment_end_datetime     timestamp without time zone   not null,
    proration_amount_in_cents_with_vat bigint                     not null,
    stripe_proration_invoice_id     varchar,
    new_stripe_subscription_id      varchar,
    credit_note_invoice_id          varchar,
    new_invoice_id                  varchar,
    creation_datetime               timestamp without time zone default current_timestamp,
    foreign key (user_id) references "user" (id),
    foreign key (from_subscription_product_id) references subscription_product (id),
    foreign key (to_subscription_product_id) references subscription_product (id)
);

create unique index if not exists user_subscription_upgrade_stripe_proration_invoice_unique_idx
    on user_subscription_upgrade (stripe_proration_invoice_id) where stripe_proration_invoice_id is not null;

create index if not exists user_subscription_upgrade_user_id_idx on user_subscription_upgrade (user_id);

create table if not exists user_subscription_upgrade_status_history
(
    id                          varchar primary key default uuid_generate_v4(),
    user_subscription_upgrade_id varchar                     not null,
    status                      subscription_upgrade_status  not null,
    creation_datetime           timestamp without time zone  not null,
    foreign key (user_subscription_upgrade_id) references user_subscription_upgrade (id) on delete cascade
);

create index if not exists user_subscription_upgrade_status_history_upgrade_id_idx
    on user_subscription_upgrade_status_history (user_subscription_upgrade_id);
