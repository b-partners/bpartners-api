create table if not exists area_picture
(
    id                    varchar
        constraint area_picture_pk primary key default uuid_generate_v4(),
    id_user           varchar not null,
    id_file_info          varchar not null,
    address               varchar not null,
    longitude numeric not null,
    latitude  numeric not null,
    created_at            timestamp with time zone default current_timestamp,
    updated_at            timestamp with time zone default current_timestamp,
    zoom_level            varchar not null,
    layer                 varchar not null,
    constraint area_picture_user_fk foreign key (id_user) references "user" (id),
    constraint area_picture_file_info_fk foreign key (id_file_info) references "file_info" (id)
);

create or replace function update_updated_at_area_picture()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language 'plpgsql';

create trigger update_area_picture_updated_at
    before update
    on
        "area_picture"
    for each row
execute procedure update_updated_at_area_picture();