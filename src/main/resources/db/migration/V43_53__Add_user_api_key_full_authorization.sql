create table if not exists user_api_key_full_authorization
(
    id                varchar primary key,
    id_user           varchar references "user" (id),
    creation_datetime timestamp without time zone default current_timestamp
);