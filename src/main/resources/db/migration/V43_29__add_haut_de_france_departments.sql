INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES
    ('68f82be1-d509-4f8d-8de7-eca5e3040a22', 'GEOSERVER', 2009, 'HautDeFrance_All_region_5cm', 'aisne', 'HOUSES_0', 5),
    ('094e82ec-6021-4b06-8311-0bddb149bd74', 'GEOSERVER', 2015, 'HautDeFrance_All_region_5cm', 'pas-de-calais', 'HOUSES_0', 5),
    ('5b4030b3-8b79-41b3-b5f6-989a8ee721b6', 'GEOSERVER', 2013, 'HautDeFrance_All_region_5cm', 'oise', 'HOUSES_0', 5),
    ('c885ab79-f8eb-48dc-aa75-920d46919344', 'GEOSERVER', 2008, 'HautDeFrance_All_region_5cm', 'somme', 'HOUSES_0', 5),
    ('a53b9ccd-eaed-44b2-9470-a53231908add', 'GEOSERVER', 2022, 'HautDeFrance_All_region_5cm', 'nord', 'HOUSES_0', 5)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm;


UPDATE area_picture_map_layer set name='ILLE-ET-VILAINE_2022_5cm'
      where id='d190ab02-3b9f-49bc-b7f1-e3f90e9c3147';