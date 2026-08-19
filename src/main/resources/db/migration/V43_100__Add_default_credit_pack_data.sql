insert into credit_pack (id, code, description, credit_purchase_type, credits, validity_days,
                         most_chosen, deprecated, display_position)
select v.id, v.code, v.description, v.credit_purchase_type, v.credits, v.validity_days,
       v.most_chosen, v.deprecated, v.display_position
from (values ('29bea0d7-fe0b-4332-aac0-385492b54ce1', 'ANALYSES_5'::credit_code,
              '5 analyses de toiture', 'PACK'::credit_purchase_type, 5::bigint, null::integer,
              false, false, 2),
             ('4ac8b6c0-2770-4493-bd21-cbbc955df423', 'ANALYSES_10'::credit_code,
              '10 analyses de toiture', 'PACK'::credit_purchase_type, 10::bigint, null::integer,
              true, false, 3),
             ('d951f889-6084-490d-aa98-27754533f672', 'ANALYSES_20'::credit_code,
              '20 analyses de toiture', 'PACK'::credit_purchase_type, 20::bigint, null::integer,
              false, false, 4),
             ('a1309dec-f410-4812-9ce0-144e288d4526', 'PACK_CUSTOM'::credit_code,
              'Nombre d''analyses au choix', 'CUSTOM'::credit_purchase_type, null::bigint,
              null::integer, false, false, 1))
         as v(id, code, description, credit_purchase_type, credits, validity_days, most_chosen,
              deprecated, display_position)
where not exists (select 1 from credit_pack cp where cp.code = v.code);
