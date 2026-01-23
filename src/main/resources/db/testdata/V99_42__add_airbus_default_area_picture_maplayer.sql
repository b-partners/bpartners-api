ALTER TYPE area_picture_source ADD VALUE 'AIRBUS';
COMMIT;

insert into "area_picture_map_layer" (id, source, year, name, departement_name, precision_level_in_cm, maximum_zoom_level)
VALUES ('532ea7da-918e-4bb7-bc34-e167a3829e19', 'AIRBUS', '2025', 'AIRBUS.PNEO', 'ALL', '30', 'BUILDING');