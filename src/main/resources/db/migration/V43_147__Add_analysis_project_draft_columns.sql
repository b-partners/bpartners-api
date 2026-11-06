alter table if exists analysis_project
    add column if not exists area_picture_id varchar,
    add column if not exists prospect_id     varchar;

create index if not exists analysis_project_area_picture_id_idx
    on analysis_project (area_picture_id)
    where area_picture_id is not null;

insert into analysis_project (id, user_id, project_key, address, area_picture_id, creation_datetime)
select uuid_generate_v4(),
       ap.id_user,
       ap.id,
       ap.address,
       ap.id,
       min(apa.creation_datetime)
from area_picture_annotation apa
         join area_picture ap on ap.id = apa.id_area_picture
where apa.is_draft
  and not exists(select
                 from analysis_project p
                 where p.user_id = ap.id_user
                   and (p.project_key = ap.id or p.area_picture_id = ap.id))
group by ap.id_user, ap.id, ap.address
on conflict do nothing;
