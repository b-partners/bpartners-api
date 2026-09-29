create table if not exists subscription_invoice_period
(
    id                    varchar primary key         default uuid_generate_v4(),
    user_id               varchar                     not null,
    invoice_id            varchar                     not null,
    period_start_datetime timestamp without time zone not null,
    period_end_datetime   timestamp without time zone not null,
    creation_datetime     timestamp without time zone default current_timestamp,
    foreign key (user_id) references "user" (id)
);

create unique index if not exists subscription_invoice_period_invoice_id_unique_idx
    on subscription_invoice_period (invoice_id);

create index if not exists subscription_invoice_period_user_id_idx
    on subscription_invoice_period (user_id);

insert into subscription_invoice_period (user_id, invoice_id, period_start_datetime,
                                         period_end_datetime)
select distinct on (p.invoice_id) p.user_id,
                                  p.invoice_id,
                                  p.invoiced_period_start_datetime,
                                  p.invoiced_period_end_datetime
from subscription_payment p
where p.invoice_id is not null
  and p.invoiced_period_start_datetime is not null
  and p.invoiced_period_end_datetime is not null
order by p.invoice_id, p.invoiced_period_end_datetime desc
on conflict (invoice_id) do nothing;
