alter table area_picture_annotation_instance
    add column if not exists height numeric;

alter table area_picture_annotation_instance
    add column if not exists global_rate_value numeric;

alter table area_picture_annotation_instance
    add column if not exists revetement_1 varchar;

alter table area_picture_annotation_instance
    add column if not exists revetement_2 varchar;
