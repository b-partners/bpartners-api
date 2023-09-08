create table if not exists calendar_stored_credential
(
    id                           varchar
        constraint cal_stored_credential_pk primary key not null default uuid_generate_v4
        (),
    id_user                      varchar,
    access_token                 varchar,
    refresh_token                varchar,
    expiration_time_milliseconds bigint,
    creation_datetime            timestamp without time zone     default current_timestamp,
    constraint cal_stored_credential_user_fk foreign key (id_user)
        references "user" (id)
);