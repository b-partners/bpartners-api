INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, precision_level_in_cm, maximum_zoom_level)
VALUES
    ('726f5b3b-d23b-40c3-b38e-68a43d7ae155', 'GEOSERVER', '2023', 'cite:PCRS.LAMB93', 'ALL', '20', 'HOUSES_0'),
    ('2f343dba-dd5f-4895-9006-49472f576c02', 'GEOSERVER', '2023', 'cite:PHOTO_AERIENNE', 'ALL', '20', 'HOUSES_0')
    ON CONFLICT (id)
DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level;
