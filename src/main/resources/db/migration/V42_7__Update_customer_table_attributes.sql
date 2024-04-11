alter table customer
    add column is_converted boolean not null default false;
-- all customers before this migration are currently converted ones.
update customer
set is_converted = true
where customer.is_converted = true;