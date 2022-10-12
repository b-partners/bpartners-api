do
$$
begin
        if not exists(select from pg_type where typname = 'invoice_status') then
create type "invoice_status" as enum ('DRAFT','PROPOSAL','CONFIRMED');
end if;
end
$$;

create table if not exists "invoice"
(
    id varchar
    constraint invoice_pk primary key default uuid_generate_v4(),
    id_account varchar not null,
    id_customer varchar not null,
    "ref" varchar not null,
    vat integer not null,
    invoice_date date not null,
    to_pay_at date not null,
    percentage_reduction integer,
    amount_reduction integer,
    status invoice_status not null,
    constraint invoice_customer_fk foreign key(id_customer)
    references "customer"(id),
    constraint invoice_ref_unique unique(id_account, "ref")
);
