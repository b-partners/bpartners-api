alter table "customer"
    add column if not exists latest_full_address varchar;

update "customer"
set latest_full_address = address || ' ' || zip_code || ' ' || city || ' ' || country
where latest_full_address is null;