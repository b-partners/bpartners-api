alter table "prospect_evaluation_job"
    add column if not exists metadata_string varchar default '{}';

update "prospect_evaluation_job"
set metadata_string = '{}'
where metadata_string is null;