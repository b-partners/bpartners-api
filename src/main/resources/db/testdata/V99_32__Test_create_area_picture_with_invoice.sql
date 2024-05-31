insert into "area_picture"
(id, id_user, id_file_info, address, longitude, latitude, created_at, updated_at, zoom_level, id_layer, filename,
 id_prospect, is_extended, score, geopositions)
values ('area_picture_1_id', 'joe_doe_id', 'montauban_5cm_544729_383060.jpg', 'Montauban Address', '0.148409',
        '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8',
        'montauban_5cm_544729_383060.jpg', 'prospect1_id', false, 90, '[
    {
      "score": 90.0,
      "longitude": 0.148409,
      "latitude": 45.644018
    },
    {
      "score": 30.0,
      "longitude": 0.148409,
      "latitude": 45.644018
    },
    {
      "score": 40.0,
      "longitude": 0.148409,
      "latitude": 45.644018
    }
  ]'),
       ('area_picture_2_id', 'joe_doe_id', 'mulhouse_1_5cm_544729_383060.jpg', 'Cannes Address', '0.148409',
        '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8',
        'mulhouse_5cm_544729_383060_extended.jpg', 'prospect1_id', true, 60, '[
         {
           "score": 60.0,
           "longitude": 0.148409,
           "latitude": 45.644018
         }
       ]'),
       ('area_picture_3_id', 'jane_doe_id', 'mulhouse_2_5cm_544729_383060.jpg', 'Cannes Address', '0.148409',
        '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8',
        'mulhouse_5cm_544729_383060.jpg', 'prospect2_id', false, 50, '[
         {
           "score": 50.0,
           "longitude": 0.148409,
           "latitude": 45.644018
         },
         {
           "score": 40.0,
           "longitude": 0.148409,
           "latitude": 45.644018
         }
       ]');
insert into "invoice"
(id, id_user, title, "ref", id_customer, sending_date, validity_date, to_pay_at, status, comment,
 "created_datetime", payment_url, file_id, payment_type, id_area_picture)
values ('invoice9_id', 'joe_doe_id', 'Facture ' ||
                                     'plomberie 9', 'BP009',
        'customer2_id',
        '2022-09-10',
        '2022-10-14', '2022-10-10', 'CONFIRMED', null, '2022-01-01T03:00:00.00Z',
        'https://connect-v2-sbx.fintecture.com', null, 'CASH', 'area_picture_1_id');
