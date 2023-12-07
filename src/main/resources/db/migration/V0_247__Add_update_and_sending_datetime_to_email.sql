alter table "email"
    add column if not exists sending_datetime timestamp without time zone,
    add column if not exists updated_at       timestamp without time zone;

delete
from "email"
where updated_at is null;