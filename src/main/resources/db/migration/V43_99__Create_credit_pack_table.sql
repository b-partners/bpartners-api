do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_purchase_type') then
            create type credit_purchase_type as enum ('PACK', 'CUSTOM');
        end if;
    end
$$;

do
$$
    begin
        if not exists(select from pg_type where typname = 'credit_code') then
            create type credit_code as enum ('ANALYSES_5', 'ANALYSES_10','ANALYSES_20', 'PACK_CUSTOM');
        end if;
    end
$$;

create table if not exists credit_pack
(
    id                   varchar primary key           default uuid_generate_v4(),
    code                 credit_code          not null,
    description          varchar,
    credit_purchase_type credit_purchase_type not null,
    credits              bigint,
    validity_days        integer,
    most_chosen          boolean              not null default false,
    deprecated           boolean              not null default false,
    display_position     integer,
    creation_datetime    timestamp without time zone   default current_timestamp
);

insert into credit_pack (id, code, description, credit_purchase_type, credits, validity_days,
                         most_chosen, deprecated, display_position)
select v.id, v.code, v.description, v.credit_purchase_type, v.credits, v.validity_days,
       v.most_chosen, v.deprecated, v.display_position
from (values ('a1c1e2f0-0000-4000-a000-000000000005', 'ANALYSES_5'::credit_code,
              '5 analyses de toiture', 'PACK'::credit_purchase_type, 5::bigint, null::integer,
              false, false, 1),
             ('a1c1e2f0-0000-4000-a000-000000000010', 'ANALYSES_10'::credit_code,
              '10 analyses de toiture', 'PACK'::credit_purchase_type, 10::bigint, null::integer,
              true, false, 2),
             ('a1c1e2f0-0000-4000-a000-000000000020', 'ANALYSES_20'::credit_code,
              '20 analyses de toiture', 'PACK'::credit_purchase_type, 20::bigint, null::integer,
              false, false, 3),
             ('a1c1e2f0-0000-4000-a000-0000000000c0', 'PACK_CUSTOM'::credit_code,
              'Nombre d''analyses au choix', 'CUSTOM'::credit_purchase_type, null::bigint,
              null::integer, false, false, 4))
         as v(id, code, description, credit_purchase_type, credits, validity_days, most_chosen,
              deprecated, display_position)
where not exists (select 1 from credit_pack cp where cp.code = v.code);