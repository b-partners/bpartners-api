INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES
    ('7fa02d65-07f6-4c04-8ca2-4c6decc6fd1e', 'GEOSERVER', 2021, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Ain', 'HOUSES_0', 5),
    ('1da3f594-be8a-482d-9e93-378193989c79', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Allier', 'HOUSES_0', 5),
    ('8b7efb53-a6d9-4d8b-8eae-012d22f97e8a', 'GEOSERVER', 2023, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Ardèche', 'HOUSES_0', 5),
    ('7f5f072c-7be7-4b03-bb69-f6ebf41fa48c', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Cantal', 'HOUSES_0', 5),
    ('c36ba1b3-10f6-4d3c-9d62-e908ea603805', 'GEOSERVER', 2023, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Drôme', 'HOUSES_0', 5),
    ('33d4bbec-5cb9-473c-bf10-7e3e3ff12e16', 'GEOSERVER', 2021, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Isère', 'HOUSES_0', 5),
    ('7015d38f-8ded-43c4-9988-bbb190b6fd3f', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Loire', 'HOUSES_0', 5),
    ('da7e4966-18b3-4d81-86bf-a88f1b033ba4', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Haute-Loire', 'HOUSES_0', 5),
    ('1babb25f-f906-4262-8431-f5f5735868a6', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Puy-de-Dôme', 'HOUSES_0', 5),
    ('1871b03b-8d58-4ea5-85bb-3ca021fbc6ad', 'GEOSERVER', 2023, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Rhône', 'HOUSES_0', 5),
    ('6bcc0ce2-f08b-4306-8797-d18249bd180b', 'GEOSERVER', 2022, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Savoie', 'HOUSES_0', 5),
    ('49c4dd99-ab4b-4a8c-8ef4-f9ce0861bce7', 'GEOSERVER', 2023, 'Auvergne_Rhone_Alpes_All_region_5cm', 'Haute-Savoie', 'HOUSES_0', 5)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm;