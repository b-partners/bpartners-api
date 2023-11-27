alter table "transaction"
    add column if not exists id_user varchar;
alter table "transaction"
    add column if not exists id_bank varchar;

alter table "transaction"
    drop constraint if exists transaction_user_fk;
alter table "transaction"
    add constraint transaction_user_fk foreign key (id_user) references "user" (id);
alter table "transaction"
    drop constraint if exists transaction_bank_fk;
alter table "transaction"
    add constraint transaction_bank_fk foreign key (id_bank) references "bank" (id);

update "transaction"
set id_user = associated_user,
    id_bank = associated_bank
from (select u.id as associated_user
           , t.id as transaction_id
           , b.id as associated_bank
      from "user" u
               join account a on u.id = a.id_user
               join "transaction" t on a.id = t.id_account
               join bank b on cast(b.external_id as varchar) = a.id_bank) req1
where "transaction".id = transaction_id;;