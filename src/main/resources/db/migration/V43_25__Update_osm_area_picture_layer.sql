UPDATE "area_picture" SET id_layer='9a4bd8b7-556b-49a1-bea0-c35e961dab64'
where area_picture.id_layer = '2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8';

DELETE from "area_picture_map_layer" where id='2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8';
DELETE from "area_picture" where id='2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8';