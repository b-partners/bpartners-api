alter table "account"
    drop column if exists external_id;

update "account"
set id_bank = b.id
from bank b
where cast(b.external_id as varchar) = "account".id_bank;
