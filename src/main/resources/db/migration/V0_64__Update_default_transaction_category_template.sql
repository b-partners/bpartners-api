insert into "transaction_category_template"
    (id, "type", vat, transaction_type, description, other)
values ('aa66239c-2be3-4c04-b260-49b207951ecf', 'Autres produits', '0/1', 'INCOME', 'Autres ' ||
                                                                                    'produits',
        true),
       ('76b0b470-d184-43a7-b1d5-9389bc6d73e7', 'Autres dépenses', '0/1', 'OUTCOME', 'Autres ' ||
                                                                                     'dépenses',
        true);

update "transaction_category_template"
set description = 'Prestations ou ventes soumises à 20% de TVA'
where id = '65a891b8-43e3-402a-9b36-073a0fed680e';

update "transaction_category_template"
set description = 'Prestations ou ventes soumises à 10% de TVA'
where id = '2b13725e-42c5-4e51-89a6-58a84f5f5072';

update "transaction_category_template"
set description = 'Prestations ou ventes soumises à 8,5% de TVA'
where id = '376d5b12-a14e-49fd-b137-4c4efb350823';

update "transaction_category_template"
set description = 'Prestations ou ventes soumises à 5,5% de TVA'
where id = '7a11a45e-1f25-4db5-b5b0-482499527443';

update "transaction_category_template"
set description = 'Prestations ou ventes soumises à 2,1% de TVA'
where id = 'f2908415-390e-4858-a4fa-1bbc84e3bd4d';

update "transaction_category_template"
set description = 'Achat marchandise soumis à 20% TVA'
where id = '6e1767d9-b35d-411a-8d23-b725ecd00921';

update "transaction_category_template"
set description = 'Achat marchandise soumis à 10% TVA'
where id = 'c301e286-5d94-4986-9c4b-3dbf4b88aae4';

update "transaction_category_template"
set description = 'Achat marchandise soumis à 5,5% TVA'
where id = '7e97bd2a-138c-43b7-865e-52475176acd7';

update "transaction_category_template"
set description = 'Achat marchandise soumis à 2,1% TVA'
where id = '91fcdb54-b20b-4484-922a-02cdb15a685f';

update "transaction_category_template"
set description = 'Produits des activités annexes'
where id = 'f1d56175-5631-43f8-b15b-12de721e32e3';

update "transaction_category_template"
set description = 'Apport argent personnel'
where id = '0c1c4653-30ff-4e5e-8585-eba51426bfd3';

update "transaction_category_template"
set description = 'Déblocage emprunt'
where id = '9493cc4c-b8fd-4b49-8fab-7c311ca6a065';

update "transaction_category_template"
set description = 'Intérêts reçus'
where id = '51321a76-3b58-4424-a34a-3a77da8d467b';

update "transaction_category_template"
set description = 'Virement compte à compte'
where id = 'b7563cee-476f-45ea-b6ae-2deac4df3419';

update "transaction_category_template"
set description = 'Aide perçues pour les apprentis'
where id = '2c2549bb-9c12-48b8-91aa-bfeab4f3ec83';

update "transaction_category_template"
set description = 'Achat marchandise exonérée'
where id = 'a6c792f5-5c00-4ad5-81a6-a851eb506b56';

update "transaction_category_template"
set description = 'Commissions prestataires',
    "type"      = 'Commissions'
where id = '6be9da5f-0197-41d4-8dcd-ff243cfad85b';

update "transaction_category_template"
set description = 'Cotisations professionnelles'
where id = '5425436d-719f-4392-bde1-3d2d20d1b52f';

update "transaction_category_template"
set description = 'Don'
where id = '868d64e8-8e3b-41c4-bf74-196ac226b89a';

update "transaction_category_template"
set description = 'Cadeaux clients'
where id = '604b5e1b-d6a0-4323-9def-e247e140e043';

update "transaction_category_template"
set description = 'Dépenses entretiens matériels voitures, machines, carburant'
where id = 'f8d196d5-170b-4701-805c-c005b70e325e';

update "transaction_category_template"
set description = 'Dépenses fournitures'
where id = '41fb0b5d-eb27-410a-b7c9-09bf1c5c3fbc';

update "transaction_category_template"
set description = 'Dépenses pub'
where id = '45b82104-b93d-4458-ab16-b0ad6d7168da';

update "transaction_category_template"
set description = 'Dépenses sous traitance/ interim'
where id = '5118f4f4-d3c4-4e2d-8bc4-b452511b914e';

update "transaction_category_template"
set description = 'Abonnement Energie, téléphone'
where id = '71c211ad-be52-4603-844e-425c70aad08e';

update "transaction_category_template"
set description = 'Commission bancaire'
where id = '5e43eece-0c67-410e-9a81-c6fd4291e7a9';

update "transaction_category_template"
set description = 'Intérêt bancaire'
where id = 'b306bd5a-48db-4583-922f-50e3360492f3';

update "transaction_category_template"
set description = 'Impôt payé'
where id = '0e9f041d-2570-4b34-b842-b7a5ba2e43a6';

update "transaction_category_template"
set description = 'TVA payé'
where id = 'b9b0b57b-2128-49ea-944d-29c6f232a559';

update "transaction_category_template"
set description = 'Loyer tous types'
where id = '2e588628-de8f-42b7-bcfd-2eeae9e3b271';

delete
from "transaction_category_template"
where id = 'fc7cd1a8-ff34-4f54-9ce5-e1861035aee3';