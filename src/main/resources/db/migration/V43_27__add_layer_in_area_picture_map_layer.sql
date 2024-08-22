INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES
    ('b37d63d5-2bf1-4688-8616-5e452ec9a72c', 'GEOSERVER', 2020, 'Auvergne_RhoneAlpes_All_region_2023_5cm', 'Auvergne Rhone Alpes', 'HOUSES_0', 5),
    ('adda80c5-a0b7-48c0-8d1b-617dc6a04164', 'GEOSERVER', 2020, 'HautDeFrance_All_region_2023_5cm', 'Haut de france', 'HOUSES_0', 5)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm;

UPDATE area_picture_map_layer set name='TARN-ET-GARONNE_2020_5CM' where id='f106171e-fb1d-4822-ac65-ddf6a69221ac';