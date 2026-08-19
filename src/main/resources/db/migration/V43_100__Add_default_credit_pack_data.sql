insert into credit_pack (id, code, description, credit_purchase_type, credits, validity_days,
                         most_chosen, deprecated, display_position)
values ('29bea0d7-fe0b-4332-aac0-385492b54ce1', '5_ANALYSES', '5 analyses de toiture', 'PACK', 5, null,
        false, false, 2),
       ('4ac8b6c0-2770-4493-bd21-cbbc955df423', '10_ANALYSES', '10 analyses de toiture', 'PACK', 10,
        null, true, false, 3),
       ('d951f889-6084-490d-aa98-27754533f672', '20_ANALYSES', '20 analyses de toiture', 'PACK', 20,
        null, false, false, 4),
       ('a1309dec-f410-4812-9ce0-144e288d4526', 'PACK_CUSTOM', 'Nombre d''analyses au choix',
        'CUSTOM', null, null, false, false, 1);
