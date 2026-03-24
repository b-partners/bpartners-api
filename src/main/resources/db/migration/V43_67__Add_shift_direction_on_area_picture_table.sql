do
$$
begin
        if not exists(select from pg_type where typname = 'shift_direction') then
create type shift_direction
    as enum ('RIGHT_LEFT_SIDE', 'UP_DOWN_SIDE');
end if;
end
$$;

alter table area_picture
    add column if not exists shift_direction shift_direction;