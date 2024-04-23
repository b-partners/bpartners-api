insert into "area_picture"
(id, id_user, id_file_info, address, longitude, latitude, created_at, updated_at, zoom_level, layer, filename, id_prospect)
values ('area_picture_1_id', 'joe_doe_id', 'montauban_5cm_544729_383060.jpg', 'Montauban Address', '0.148409', '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', 'tous_fr', 'montauban_5cm_544729_383060.jpg', 'prospect1_id'),
       ('area_picture_2_id', 'joe_doe_id', 'mulhouse_1_5cm_544729_383060.jpg', 'Cannes Address', '0.148409', '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', 'tous_fr', 'mulhouse_5cm_544729_383060.jpg', 'prospect1_id'),
       ('area_picture_3_id', 'jane_doe_id', 'mulhouse_2_5cm_544729_383060.jpg', 'Cannes Address', '0.148409', '45.644018',
        '2022-01-08T01:00:00.00Z', '2022-01-08T01:00:00.00Z', 'HOUSES_0', 'tous_fr', 'mulhouse_5cm_544729_383060.jpg', 'prospect2_id');
insert into "invoice"
(id, id_user, title, "ref", id_customer, sending_date, validity_date, to_pay_at, status, comment,
 "created_datetime", payment_url, file_id, payment_type, id_area_picture)
values ('invoice9_id', 'joe_doe_id', 'Facture ' ||
                                     'plomberie 9', 'BP009',
        'customer2_id',
        '2022-09-10',
        '2022-10-14', '2022-10-10', 'CONFIRMED', null, '2022-01-01T03:00:00.00Z',
        'https://connect-v2-sbx.fintecture.com', null, 'CASH', 'area_picture_1_id');