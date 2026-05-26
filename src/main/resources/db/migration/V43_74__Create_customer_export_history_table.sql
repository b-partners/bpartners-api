create table if not exists customer_export_history
(
    id                    varchar primary key         default uuid_generate_v4(),
    user_owner_identifier varchar references "user" (id),
    file_key              varchar,
    additional_properties jsonb,
    creation_datetime     timestamp without time zone default now()

);