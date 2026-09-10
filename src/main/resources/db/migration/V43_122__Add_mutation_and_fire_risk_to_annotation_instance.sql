alter table area_picture_annotation_instance
    add column if not exists mutation varchar;

alter table area_picture_annotation_instance
    add column if not exists fire_risk varchar;
