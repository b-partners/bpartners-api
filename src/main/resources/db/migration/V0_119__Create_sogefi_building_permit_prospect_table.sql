create table if not exists "sogefi_building_permit_prospect"
(
    id          varchar
        constraint sogefi_building_permit_prospect_pk primary key
        default uuid_generate_v4(),
    id_prospect varchar,
    constraint fk_sogefi_building_permit_prospect foreign key (id_prospect) references prospect
        ("id"),
    id_sogefi bigint not null,
    geojson_type varchar,
    geojson_latitude decimal,
    geojson_longitude decimal
);
