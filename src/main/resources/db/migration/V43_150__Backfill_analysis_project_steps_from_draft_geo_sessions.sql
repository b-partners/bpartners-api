create or replace function pg_temp.geo_session_of(annotation_properties jsonb) returns jsonb
    language plpgsql
    immutable as
$$
declare
    saved jsonb := annotation_properties -> 'geoSession';
begin
    if saved is null or jsonb_typeof(saved) = 'null' then
        return null;
    end if;
    if jsonb_typeof(saved) = 'string' then
        begin
            return (saved #>> '{}')::jsonb;
        exception
            when others then return null;
        end;
    end if;
    return saved;
end;
$$;

with draft_session as (select apa.id_area_picture,
                              apa.creation_datetime,
                              pg_temp.geo_session_of(apa.properties) as geo_session
                       from area_picture_annotation apa
                       where apa.is_draft
                         and apa.properties -> 'geoSession' is not null),
     evidence as (select p.id,
                         bool_or(jsonb_typeof(s.geo_session #> '{state,analyse}') = 'object')  as has_2d,
                         bool_or(jsonb_typeof(s.geo_session #> '{state,cityJson}') = 'object') as has_3d,
                         min(s.creation_datetime)                                             as seen_datetime
                  from analysis_project p
                           join draft_session s
                                on s.id_area_picture = p.area_picture_id
                                    or (jsonb_typeof(s.geo_session) = 'object'
                                        and s.geo_session ->> 'sessionId' = p.project_key)
                  group by p.id)
update analysis_project p
set analysis_2d_datetime     = case
                                   when e.has_2d then coalesce(p.analysis_2d_datetime, e.seen_datetime)
                                   else p.analysis_2d_datetime end,
    model_3d_datetime        = case
                                   when e.has_3d then coalesce(p.model_3d_datetime, e.seen_datetime)
                                   else p.model_3d_datetime end,
    credit_consumed_datetime = coalesce(p.credit_consumed_datetime, e.seen_datetime)
from evidence e
where e.id = p.id
  and ((e.has_2d and p.analysis_2d_datetime is null)
    or (e.has_3d and p.model_3d_datetime is null));

drop function if exists pg_temp.geo_session_of(jsonb);
