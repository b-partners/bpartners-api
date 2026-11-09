alter table if exists "subscription_consumption_log"
    rename to "user_activity";

do
$$
    begin
        if exists(select
                  from information_schema.columns
                  where table_name = 'user_activity'
                    and column_name = 'consumption_type') then
            alter table "user_activity"
                rename column consumption_type to activity_type;
            alter table "user_activity"
                rename column consumption_unit to activity_unit;
            alter table "user_activity"
                rename column usage_metric to quantity;
            alter table "user_activity"
                rename column "comment" to description;
        end if;
    end
$$;

do
$$
    begin
        if exists(select from pg_type where typname = 'subscription_consumption_type') then
            alter type subscription_consumption_type rename to user_activity_type;
        end if;
        if exists(select from pg_type where typname = 'subscription_consumption_unit') then
            alter type subscription_consumption_unit rename to user_activity_unit;
        end if;
    end
$$;

alter type user_activity_type rename value 'ROOF_ANALYSIS' to 'ROOF_DETECTION_2D';
alter type user_activity_type rename value 'IMAGE_ACCESS' to 'AERIAL_IMAGE_ACCESS';
