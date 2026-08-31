alter table if exists "detection_tracking"
    add column if not exists "detection_identifier" varchar;

create unique index if not exists detection_tracking_detection_identifier_unique_idx
    on "detection_tracking" ("detection_identifier")
    where "detection_identifier" is not null;
