ALTER TYPE area_picture_source ADD VALUE 'GEOSERVER_IGN';
COMMIT;
INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES ('1cccfc17-cbef-4320-bdfa-0d1920b91f11', 'GEOSERVER_IGN', 2023, 'ORTHOIMAGERY.ORTHOPHOTOS', 'ALL', 'HOUSES_0', 20);

UPDATE "area_picture" SET id_layer='1cccfc17-cbef-4320-bdfa-0d1920b91f11'
where area_picture.id_layer = '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8';