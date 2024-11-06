INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES

    ('e8bed0dc-5ada-4912-a66a-3bb1d8b9bf72', 'GEOSERVER', 2018, 'MARNES_2018_5cm', 'marne', 'HOUSES_0', 5),
    ('cd5298f8-98a4-45d6-ac37-23123b29b8a7', 'GEOSERVER', 2022, 'MARNES_2022_5cm', 'marne', 'HOUSES_0', 5),
    ('f2ced6bf-b0b4-4923-b12f-922a06742cdd', 'GEOSERVER', 2021, 'MARNES_2021_5cm', 'marne', 'HOUSES_0', 5)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
                            year = EXCLUDED.year,
                            name = EXCLUDED.name,
                            departement_name = EXCLUDED.departement_name,
                            maximum_zoom_level = EXCLUDED.maximum_zoom_level,
                            precision_level_in_cm = EXCLUDED.precision_level_in_cm;