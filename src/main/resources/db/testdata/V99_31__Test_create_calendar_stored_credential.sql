insert into "calendar_stored_credential"
(id, id_user, access_token, refresh_token, expiration_time_milliseconds, creation_datetime)
values ('calendar_stored_credential1', 'joe_doe_id', 'access_token', null,
        extract(epoch from (current_timestamp + interval '3600000 milliseconds'))::bigint * 1000,
        current_timestamp at time zone 'UTC');