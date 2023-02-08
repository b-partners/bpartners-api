create table if not exists "prospect_conversion"
(
    id                varchar
        constraint prospect_conversion_pk primary key not null default uuid_generate_v4(),
    id_account_holder varchar                         not null,
    constraint fk_prospect_conversion_account_holder foreign key (id_account_holder)
        references account_holder (id),
    id_prospect       varchar                         not null,
    constraint fk_prospect_conversion_prospect foreign key (id_prospect)
        references prospect (id),
    email_sent_at     varchar,
    email_replied_at  varchar
);
create index if not exists "prospect_conversion_account_holder_index" on prospect_conversion
    (id_account_holder);
create index if not exists "prospect_conversion_prospect" on prospect_conversion (id_prospect);