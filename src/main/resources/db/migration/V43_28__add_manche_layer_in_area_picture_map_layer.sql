INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES
    ('e68bdc8c-62a3-4cd1-95bb-82ece07f376d', 'GEOSERVER', 2022, 'MANCHE_2022_5CM', 'MANCHE', 'HOUSES_0', 5)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm;

UPDATE area_picture_map_layer set year=2023
      where id='b37d63d5-2bf1-4688-8616-5e452ec9a72c'
        and id='adda80c5-a0b7-48c0-8d1b-617dc6a04164';

UPDATE area_picture_map_layer set departement_name='marne'
      where id='e8bed0dc-5ada-4912-a66a-3bb1d8b9bf72';