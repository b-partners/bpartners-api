alter table "customer" add column first_name varchar;
alter table "customer" add column last_name varchar;

do
$$
    declare n record;
begin
for n in select * from "customer"
                           loop
update "customer" set first_name =  split_part(n.name, ' ', '1') where id = n.id;
update "customer" set last_name =  case when
    split_part(n.name, split_part(n.name, ' ', '1'), '2') = '' then null
    else split_part(n.name, split_part(n.name, ' ', '1'), '2') end
                  where id = n.id;
end loop;
end;
$$
;

alter table "customer" drop column "name";
