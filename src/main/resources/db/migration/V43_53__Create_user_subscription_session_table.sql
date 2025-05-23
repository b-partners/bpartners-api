do
$$
begin
    if not exists(select from pg_type where typname = 'session_mode') then
        create type session_mode as enum ('SUBSCRIPTION', 'SETUP');
    end if;
end
$$;


create table if not exists user_subscription_session
(
    id                          varchar primary key,
    user_id                     varchar references "user" (id),
    session_id                  varchar ,
    session_mode                session_mode,
    subscription_schedule_id    varchar,
    trial_until                date,
    is_cancelled boolean        default false
);