insert into "area_picture_map_layer" (id, source, year, name, departement_name, precision_level_in_cm, maximum_zoom_level)
VALUES ('2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8', 'OPENSTREETMAP', '0', 'tous_fr', 'ALL', 20, 'HOUSES_0');

alter table if exists "area_picture"
    rename column layer to id_layer;
update "area_picture"
set id_layer = '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8'
where area_picture.id_layer = 'tous_fr';