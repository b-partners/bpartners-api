alter table if exists "area_picture"
    add column id_prospect varchar;
alter table if exists "area_picture"
    add constraint area_picture_prospect_fk foreign key (id_prospect) references "prospect" (id);
create index if not exists "area_picture_user_index" on "area_picture" (id_user);
create index if not exists "area_picture_file_info_index" on area_picture (id_file_info);
create index if not exists "area_picture_prospect_index" on area_picture (id_prospect);
alter table if exists "area_picture" drop constraint if exists area_picture_user_file_name_unique;
alter table if exists "area_picture" add constraint area_picture_user_file_name_unique unique(filename, id_user, id_prospect);
