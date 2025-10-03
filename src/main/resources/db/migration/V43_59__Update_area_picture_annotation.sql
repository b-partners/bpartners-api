alter table if exists area_picture_annotation
    add column if not exists properties jsonb;
