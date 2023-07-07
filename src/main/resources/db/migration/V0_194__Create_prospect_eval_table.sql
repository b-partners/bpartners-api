do
$$
    begin
        if not exists(select from pg_type where typname = 'contact_nature') then
            create type contact_nature as enum ('PROSPECT', 'OLD_CUSTOMER', 'OTHER');
        end if;
    end
$$;

create sequence if not exists prospect_eval_info_ref_seq start with 1 increment by 1 no maxvalue;

create table if not exists "prospect_eval_info"
(
    id                    varchar
        constraint prospect_eval_info_pk primary key not null default uuid_generate_v4
        (),
    name                  varchar,
    reference             bigint,
    phone_number          varchar,
    email                 varchar,
    website               varchar,
    address               varchar,
    manager_name          varchar,
    mail_sent             varchar,
    postal_code           varchar,
    city                  varchar,
    category              varchar,
    subcategory           varchar,
    pos_longitude         numeric,
    pos_latitude          numeric,
    company_creation_date date,
    contact_nature        contact_nature                      default 'PROSPECT',
    id_account_holder     varchar                    not null,
    constraint fk_prospect_eval_info_account_holder
        foreign key (id_account_holder) references account_holder (id),
    constraint unique_prospect_eval_info_reference
        unique (reference)
);
create index if not exists "prospect_eval_info_account_holder_index" on "prospect" ("id_account_holder");