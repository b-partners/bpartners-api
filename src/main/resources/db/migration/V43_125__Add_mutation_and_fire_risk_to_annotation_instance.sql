alter table area_picture_annotation_instance
    add column if not exists mutation varchar;

alter table area_picture_annotation_instance
    add column if not exists fire_risk varchar;

alter table area_picture_annotation_instance
    add column if not exists mutation_recent_image_url varchar;

alter table area_picture_annotation_instance
    add column if not exists mutation_recent_image_date integer;

alter table area_picture_annotation_instance
    add column if not exists mutation_older_image_url varchar;

alter table area_picture_annotation_instance
    add column if not exists mutation_older_image_date integer;
