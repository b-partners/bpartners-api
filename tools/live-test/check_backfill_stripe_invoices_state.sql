with attendus(stripe_invoice_id, email) as (values
    ('in_1ULVYYAoE0lugLpBeMNVcaWR', 'cabon.sellier123@gmail.com'),
    ('in_1ULVYdAoE0lugLpB8leUWOAd', 'commercial@baticausses.fr'),
    ('in_1ULVZ7AoE0lugLpBHB2EJ0QO', 'contactadmi@applic-3d.fr'),
    ('in_1ULVaBAoE0lugLpBtjokb1Ud', 'davida.lagoz@hotmail.com'),
    ('in_1ULVaMAoE0lugLpByIqD7eBy', 'dupreznicolas_02@gmail.com'),
    ('in_1ULVatAoE0lugLpBIYsta4DA', 'hh.habitat.ls@gmail.com'),
    ('in_1ULVY5AoE0lugLpBYnQUDjZH', 'igct008s4x@gmeenramy.com'),
    ('in_1ULVbUAoE0lugLpBILGWw3nB', 'ines_bayama@gmail.com'),
    ('in_1ULVamAoE0lugLpBRwDe5T9O', 'm.brusetti@polyexpert.fr'),
    ('in_1ULVY3AoE0lugLpB7eYL5hov', 'philippe.pagenaud@reassist.fr'),
    ('in_1ULVYcAoE0lugLpBpYhhxz3B', 'pointventdjimy@gmail.com'),
    ('in_1ULValAoE0lugLpB8PhF5qEh', 'ronan.deserable@maif.fr'),
    ('in_1ULVadAoE0lugLpBnO7b9TRr', 'scarrillorosa@attila.fr'),
    ('in_1ULVanAoE0lugLpBHUNHUXhn', 'wimones142@mtupu.com'))
select a.email,
       a.stripe_invoice_id,
       sp.id                        as subscription_payment_id,
       sp.creation_datetime,
       sp.invoice_id,
       i."ref"                      as facture_ref,
       case
           when sp.id is null then 'RIEN ECRIT'
           when sp.invoice_id is null then 'PAIEMENT ORPHELIN A FACTURER'
           else 'FACTURE'
           end                      as etat
from attendus a
         left join subscription_payment sp on sp.stripe_invoice_id = a.stripe_invoice_id
         left join "invoice" i on i.id = sp.invoice_id
order by etat, a.email;
