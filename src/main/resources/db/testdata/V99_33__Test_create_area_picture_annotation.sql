insert into "area_picture_annotation" (id, creation_datetime, id_user, id_area_picture)
values ('area_picture_annotation_1_id', '2022-01-08T01:00:00.00Z', 'joe_doe_id', 'area_picture_1_id'),
       ('area_picture_annotation_2_id', '2022-01-08T01:02:00.00Z', 'joe_doe_id', 'area_picture_1_id'),
       ('area_picture_annotation_3_id', '2022-01-08T01:02:00.00Z', 'jane_doe_id', 'area_picture_3_id');

insert into "area_picture_annotation_instance" (id, slope, area, covering, wear_level, polygon, label_name,
                                                id_annotation, id_user, id_area_picture)
values ('area_picture_annotation_instance_1_id', 80, 90, 'Tuiles', 100, '{
  "points": [
    {
      "x": 1.0,
      "y": 1.0
    }
  ]
}', 'roof nord-est', 'area_picture_annotation_1_id', 'joe_doe_id', 'area_picture_1_id'),
       ('area_picture_annotation_instance_2_id', 80, 90, 'Beton', 100, '{
         "points": [
           {
             "x": 2.0,
             "y": 2.0
           }
         ]
       }', 'roof nord-est 2', 'area_picture_annotation_1_id', 'joe_doe_id', 'area_picture_1_id'),
       ('area_picture_annotation_instance_3_id', 80, 90, 'Tuiles', 100, '{
         "points": [
           {
             "x": 1.0,
             "y": 1.0
           }
         ]
       }', 'roof nord-est', 'area_picture_annotation_3_id', 'jane_doe_id', 'area_picture_3_id');