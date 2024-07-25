ALTER TYPE area_picture_source ADD VALUE 'GEOSERVER_IGN';
COMMIT;

INSERT INTO area_picture_map_layer (id, source, year, name, departement_name, maximum_zoom_level, precision_level_in_cm)
VALUES
    ('2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8', 'OPENSTREETMAP', 0, 'tous_fr', 'ALL', 'HOUSES_0', 20),
    ('e2f2bfdf-51db-4fe4-9abf-47346dad7a15', 'GEOSERVER', 2020, 'ALPES-MARITIMES_2020_5cm', 'alpes-maritimes', 'HOUSES_0', 5),
    ('7fac6cca-4976-49e2-b0ed-19899fae3540', 'GEOSERVER', 2019, 'VENDEE_2019_20cm', 'vendee', 'HOUSES_0', 20),
    ('22e9c627-ba47-4398-9a0f-a996841a28ac', 'GEOSERVER', 2023, 'GIRONDE_2023_5cm', 'gironde', 'HOUSES_0', 5),
    ('9998b18f-0d3b-403a-a1e2-283db529c5fe', 'GEOSERVER', 2023, 'FINISTERE_2023_5cm', 'finistere', 'HOUSES_0', 5),
    ('f106171e-fb1d-4822-ac65-ddf6a69221ac', 'GEOSERVER', 2020, 'TARN-ET-GARONNE_2020_5cm', 'tarn-et-garonne', 'HOUSES_0', 5),
    ('597c3b13-0f7c-4a77-aa17-b1815eb8dab3', 'GEOSERVER', 2022, 'HAUTE-GARONNE_2022_5cm', 'haute-garonne', 'HOUSES_0', 5),
    ('5643c1a8-f0ba-4076-84a3-9dc5a7f1ac1d', 'GEOSERVER', 2022, 'COTE_D''OR_2022_5cm', 'cote-d-or', 'HOUSES_0', 5),
    ('e8bed0dc-5ada-4912-a66a-3bb1d8b9bf72', 'GEOSERVER', 2018, 'MARNES_2018_5cm', 'marnes', 'HOUSES_0', 5),
    ('d190ab02-3b9f-49bc-b7f1-e3f90e9c3147', 'GEOSERVER', 2018, 'ILE-ET-VILAINE_2022_5cm', 'Ille-et-Vilaine', 'HOUSES_0', 5),
    ('cee23f9b-d3ca-4a47-b27a-d7d62b5119c7', 'GEOSERVER', 2023, 'VENDEE_2023_5cm', 'vendee', 'HOUSES_0', 5),
    ('cd5298f8-98a4-45d6-ac37-23123b29b8a7', 'GEOSERVER', 2022, 'MARNES_2022_5cm', 'marnes', 'HOUSES_0', 5),
    ('f2ced6bf-b0b4-4923-b12f-922a06742cdd', 'GEOSERVER', 2021, 'MARNES_2021_5cm', 'marnes', 'HOUSES_0', 5),
    ('9a4bd8b7-556b-49a1-bea0-c35e961dab64', 'GEOSERVER', 2023, 'FLUX_IGN_2023_20CM', 'ALL', 'HOUSES_0', 20),
    ('d309e47b-960f-4c4f-b50f-9b341eb7125b', 'GEOSERVER', 2018, 'RHONE_2018_5CM', 'RHONE', 'HOUSES_0', 5),
    ('47af6981-a5df-46f6-b9c4-dc7fc0045dc2', 'GEOSERVER', 2021, 'MANCHE_2021_5CM', 'MANCHE', 'HOUSES_0', 5),
    ('f2240b99-c268-4800-9375-15cf44b95d57', 'GEOSERVER', 2023, 'HAUTE-SAVOIE_2023_5CM', 'HAUTE-SAVOIE', 'HOUSES_0', 5),
    ('73cd18c1-ba68-492f-a391-f84dabd6f5aa', 'GEOSERVER', 2022, 'Loire-Atlantique_Nantes_Pirmil_2022_5cm', 'Loire-Atlantique', 'HOUSES_0', 5),
    ('1cccfc17-cbef-4320-bdfa-0d1920b91f11', 'GEOSERVER_IGN', 2023, 'ORTHOIMAGERY.ORTHOPHOTOS', 'ALL', 'HOUSES_0', 20)
    ON CONFLICT (id) DO UPDATE SET
    source = EXCLUDED.source,
    year = EXCLUDED.year,
    name = EXCLUDED.name,
    departement_name = EXCLUDED.departement_name,
    maximum_zoom_level = EXCLUDED.maximum_zoom_level,
    precision_level_in_cm = EXCLUDED.precision_level_in_cm;


UPDATE "area_picture" SET id_layer='1cccfc17-cbef-4320-bdfa-0d1920b91f11'
where area_picture.id_layer = '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8';
