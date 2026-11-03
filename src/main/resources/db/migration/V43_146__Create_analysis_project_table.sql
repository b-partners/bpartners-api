create table if not exists analysis_project
(
    id                         varchar primary key default uuid_generate_v4(),
    user_id                    varchar not null,
    project_key                varchar not null,
    external_reference         varchar,
    analysis_2d_datetime       timestamp without time zone,
    model_3d_datetime          timestamp without time zone,
    credit_consumed_datetime   timestamp without time zone,
    credit_transaction_id      varchar,
    creation_datetime          timestamp without time zone default current_timestamp,
    foreign key (user_id) references "user" (id),
    foreign key (credit_transaction_id) references credit_transaction (id)
);

create unique index if not exists analysis_project_user_id_project_key_unique_idx
    on analysis_project (user_id, project_key);

create unique index if not exists analysis_project_user_id_external_reference_unique_idx
    on analysis_project (user_id, external_reference)
    where external_reference is not null;

alter table if exists "detection_tracking"
    add column if not exists project_key        varchar,
    add column if not exists external_reference varchar;

insert into analysis_project (id, user_id, project_key, analysis_2d_datetime,
                              credit_consumed_datetime, creation_datetime)
select uuid_generate_v4(),
       dt.id_user,
       'legacy:' || dt.id,
       dt.creation_datetime,
       dt.creation_datetime,
       dt.creation_datetime
from "detection_tracking" dt
where dt.id_user is not null
  and exists(select from "user" u where u.id = dt.id_user)
on conflict do nothing;
