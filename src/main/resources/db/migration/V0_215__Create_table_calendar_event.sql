create table if not exists "calendar_event"
(
    id           varchar
        constraint calendar_event_pk primary key not null default uuid_generate_v4
        (),
    ete_id       varchar,
    id_user      varchar,
    id_calendar  varchar,
    summary      varchar,
    organizer    varchar,
    location     varchar,
    participants varchar,
    "from"       timestamp,
    "to"         timestamp,
    updated_at   timestamp,
    created_at   timestamp                                default current_timestamp,
    constraint calendar_event_user_fk foreign key (id_user) references "user" (id),
    constraint calendar_event_cal_fk foreign key (id_calendar) references "calendar" (id)
);