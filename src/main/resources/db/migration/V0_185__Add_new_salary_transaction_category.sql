insert into "transaction_category_template"
    (id, "type", vat, transaction_type, description, other)
select 'd786d104-dbef-4c48-8b6d-df6c0c150b75', 'Salaire', '0/1',
        'OUTCOME', 'Salaire', false
where not exists(
        select tc.id
        from "transaction_category_template" tc
        where tc.id = 'd786d104-dbef-4c48-8b6d-df6c0c150b75'
    );