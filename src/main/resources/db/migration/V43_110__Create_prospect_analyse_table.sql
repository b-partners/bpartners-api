create table if not exists "prospect_analyse"
(
    id          varchar
        constraint prospect_analyse_pk primary key default uuid_generate_v4(),
    id_prospect varchar not null,
    metadata    jsonb,
    created_at  timestamp with time zone default current_timestamp,
    updated_at  timestamp with time zone default current_timestamp,
    constraint prospect_analyse_prospect_fk foreign key (id_prospect) references "prospect" (id)
);

create index if not exists prospect_analyse_id_prospect_index on "prospect_analyse" (id_prospect);
