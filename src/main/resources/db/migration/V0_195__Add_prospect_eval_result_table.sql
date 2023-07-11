do
$$
    begin
        if not exists(select from pg_type where typname = 'prospect_eval_rule') then
            create type prospect_eval_rule as enum ('NEW_INTERVENTION', 'ROBBERY');
        end if;
    end
$$;

create table if not exists prospect_eval
(
    id                    varchar
        constraint prospect_eval_pk primary key not null default uuid_generate_v4(),
    id_prospect_eval_info varchar,
    evaluation_date       timestamp without time zone,

    rule                  prospect_eval_rule,
    individual_customer   boolean,
    professional_customer boolean,
    declared              boolean,
    intervention_address  varchar,
    intervention_distance numeric,
    prospect_rating       numeric check (prospect_rating = -1 or (prospect_rating >= 0 and prospect_rating <= 10)),
    old_customer_address  varchar,
    old_customer_distance numeric,
    customer_rating       numeric check (customer_rating = -1 or (customer_rating >= 0 and customer_rating <= 10)),
    constraint fk_prospect_eval_info foreign key (id_prospect_eval_info) references
        prospect_eval_info (id)
);