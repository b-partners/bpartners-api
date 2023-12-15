alter table "customer"
    add column if not exists "name" varchar;

update "customer"
set "name" = "first_name" || ' ' || "last_name"
where "name" is null
  and "first_name" is not null
  and "last_name" is not null;

update "customer"
set "name" = "first_name"
where "name" is null
  and "first_name" is not null
  and "last_name" is null;

update "customer"
set "name" = "last_name"
where "name" is null
  and "last_name" is null;