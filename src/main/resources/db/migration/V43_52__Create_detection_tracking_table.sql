create table if not exists detection_tracking
(
    id                     varchar primary key,
    id_user                varchar references "user" (id),
    "zone"                 varchar,
    address                varchar,
    initiator_name         varchar,
    initiator_email        varchar,
    initiator_phone_number varchar,
    creation_datetime      timestamp without time zone default current_timestamp
);