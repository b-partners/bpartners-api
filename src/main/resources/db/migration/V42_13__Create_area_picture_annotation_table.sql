create table area_picture_annotation
(
    id                varchar primary key      default uuid_generate_v4(),
    creation_datetime timestamp with time zone default current_timestamp_utc() not null,
    id_user           varchar                                                  not null,
    id_area_picture   varchar                                                  not null,
    constraint fk_annotation_user foreign key (id_user) references "user" ("id"),
    constraint fk_annotation_picture foreign key (id_area_picture) references "area_picture" ("id")
);

create table area_picture_annotation_instance
(
    id              varchar primary key default uuid_generate_v4(),
    slope           numeric,
    area            numeric,
    covering        varchar,
    wear_level       numeric,
    polygon         jsonb,
    label_name      varchar not null,
    id_annotation   varchar not null,
    id_user         varchar not null,
    id_area_picture varchar not null,
    constraint fk_annotation_instance_annotation foreign key (id_annotation) references "area_picture_annotation" ("id"),
    constraint fk_annotation_instance_user foreign key (id_user) references "user" ("id"),
    constraint fk_annotation_instance_picture foreign key (id_area_picture) references "area_picture" ("id")
);