insert into "transaction_category_template"
    (id, "type", vat, transaction_type, description, other)
values ('af52a11e-dd33-4d69-950a-7625d9500869', 'Autres', '0/1', null, 'Autres produits et ' ||
                                                                       'dépenses',
        true);

update "transaction_category" set id_transaction_category_tmpl =
    'af52a11e-dd33-4d69-950a-7625d9500869'
    where id_transaction_category_tmpl is null;