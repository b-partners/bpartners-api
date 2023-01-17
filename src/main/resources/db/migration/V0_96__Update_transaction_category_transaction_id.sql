insert into transaction(id_swan, id_account)
select id_transaction, id_account
from transaction_category;
update transaction_category
set id_transaction = subquery.id
from (select t.id, id_swan
      from transaction t
               inner join
           transaction_category tc on t.id_swan = tc.id_transaction)
         as subquery
where transaction_category.id_transaction = subquery.id_swan;
alter table transaction_category
    add constraint fk_transaction_category_transaction foreign key (id_transaction)
        references transaction (id);