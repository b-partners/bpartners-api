do
$$
    begin
        if not exists(select from pg_type where typname = 'area_picture_source') then
            create type area_picture_source as enum ('OPENSTREETMAP', 'GEOSERVER');
        end if;
    end
$$;
create table if not exists "area_picture_map_layer"
(
    id                    varchar primary key default uuid_generate_v4(),
    source                area_picture_source not null,
    year                  int                 not null,
    name                  varchar             not null,
    departement_name      varchar             not null,
    maximum_zoom_level    varchar             not null,
    precision_level_in_cm int                 not null
);
