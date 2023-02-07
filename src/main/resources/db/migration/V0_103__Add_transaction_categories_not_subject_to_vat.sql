insert into "transaction_category_template"
    (id, "type", vat, transaction_type, description, other)
values ('124f5f33-68bc-41c0-9782-0c8dd7de5956', 'Recette TVA 0%', '0/1',
        'INCOME', 'Prestations ou ventes exonérées de TVA', false),
       ('c0668696-97cb-49d3-8618-9e94273c2b03', 'Achat TVA 0%', '0/1',
        'OUTCOME', 'Achat marchandise exonérée de TVA', false);