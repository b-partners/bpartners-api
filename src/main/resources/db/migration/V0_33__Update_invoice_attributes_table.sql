alter table "invoice"
    add column created_datetime timestamp default current_timestamp;
alter table "invoice"
    add column updated_at timestamp default current_timestamp;

create or replace function update_updated_at_invoice()
    returns trigger as
$$
begin
    new.updated_at = now();
return new;
end;
$$ language 'plpgsql';

create trigger update_invoice_updated_at
    before update
    on
        "invoice"
    for each row
execute procedure update_updated_at_invoice();
