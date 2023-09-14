do
$$
    begin
        if not exists(select from pg_type where typname = 'job_status_value') then
            create type job_status_value as enum (
                'NOT_STARTED', 'IN_PROGRESS', 'FINISHED','FAILED');
        end if;
    end
$$;

do
$$
    begin
        if not exists(select from pg_type where typname = 'prospect_evaluation_job_type') then
            create type prospect_evaluation_job_type as enum (
                'CALENDAR_EVENT_CONVERSION', 'ADDRESS_CONVERSION', 'SPREADSHEET_EVALUATION');
        end if;
    end
$$;

create table if not exists "prospect_evaluation_job"
(
    id                 varchar
        constraint prospect_evaluation_job_pk primary key default uuid_generate_v4(),
    id_account_holder  varchar,
    job_status_message varchar,
    job_status         job_status_value,
    "type"             prospect_evaluation_job_type,
    started_at         timestamp without time zone,
    ended_at           timestamp without time zone,
    constraint prospect_evaluation_job_account_holder_fk foreign key (id_account_holder)
        references "account_holder" (id)
)
