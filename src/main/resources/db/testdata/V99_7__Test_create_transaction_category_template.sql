insert into "transaction_category_template"
    (id, "type", other, transaction_type)
values ('tc_tmpl1_id', 'Recette TVA 20%', false, 'INCOME'),
       ('tc_tmpl2_id', 'Recette TVA 10%', false, 'INCOME'),
       ('tc_tmpl3_id', 'Recette TVA 8,5%', false, 'INCOME'),
       ('tc_tmpl4_id', 'Recette TVA 5,5%', false, 'INCOME'),
       ('tc_tmpl5_id', 'Recette TVA 2,1%', false, 'INCOME'),
       ('tc_tmpl6_id', 'Autres dépenses', true, 'OUTCOME'),
       ('tc_tmpl7_id', 'Autres produits', true, 'INCOME'),
       ('tc_tmpl8_id', 'Achat TVA 20%', false, 'OUTCOME'),
       ('tc_tmpl9_id', 'Achat TVA 10%', false, 'OUTCOME'),
       ('tc_tmpl10_id', 'Achat TVA 8,5%', false, 'OUTCOME'),
       ('tc_tmpl11_id', 'Achat TVA 5,5%', false, 'OUTCOME'),
       ('tc_tmpl12_id', 'Achat TVA 2,1%', false, 'OUTCOME');