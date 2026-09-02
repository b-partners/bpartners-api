alter table area_picture
    alter column filename drop not null,
    alter column id_layer drop not null,
    alter column longitude drop not null,
    alter column latitude drop not null;
