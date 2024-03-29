CREATE OR REPLACE FUNCTION current_timestamp_utc() RETURNS TIMESTAMP WITHOUT TIME ZONE AS $$
BEGIN
    RETURN (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
END;
$$ LANGUAGE plpgsql;

create table if not exists "prospect_status_history"
(
    id          varchar
        constraint prospect_status_history_pk primary key not null default uuid_generate_v4
        (),
    id_prospect varchar,
    status      prospect_status,
    updated_at  timestamp without time zone                        default current_timestamp_utc(),
    constraint prospect_status_history_fk foreign key (id_prospect) references "prospect" (id)
);

insert into "prospect_status_history" (id_prospect, status, updated_at)
select id, status, current_timestamp_utc()
from "prospect" p;

alter table "prospect"
    drop column if exists "status";