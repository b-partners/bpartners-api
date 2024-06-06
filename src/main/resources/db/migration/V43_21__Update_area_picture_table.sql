alter table area_picture
    add column geopositions jsonb default '[]';
alter table area_picture
    add column score double precision default 0;
