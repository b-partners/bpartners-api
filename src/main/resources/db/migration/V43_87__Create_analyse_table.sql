create table if not exists "analyse"
(
    id          varchar
        constraint analyse_pk primary key default uuid_generate_v4(),
    id_prospect varchar not null,
    metadata    jsonb,
    created_at  timestamp with time zone default current_timestamp,
    updated_at  timestamp with time zone default current_timestamp,
    constraint analyse_prospect_fk foreign key (id_prospect) references "prospect" (id)
);

create index if not exists analyse_id_prospect_index on "analyse" (id_prospect);
