drop function get_billing_info_by_year_and_month(year integer, month integer);

create or replace function get_billing_info_by_year_and_month(year integer, month integer)
    returns table
            (
                user_id   varchar,
                first_name         varchar,
                last_name         varchar,
                amount_value numeric,
                average numeric,
                median         double precision,
                minimum_amount numeric,
                maximum_amount numeric
            )
as
$$
BEGIN
    return query
        select pr.id_user as user_id,
               u.first_name as first_name,
               u.last_name as last_name,
               (sum((cast(split_part(pr.amount, '/', 1) as numeric)
                   / cast(split_part(pr.amount, '/', 2) as
                         numeric))) / 100) as amount_value,
               (avg(convert_fraction_to_numeric(pr.amount)))                     as average,
               percentile_cont(0.5) WITHIN GROUP ( ORDER BY convert_fraction_to_numeric(pr.amount))
                   as median,
               min(convert_fraction_to_numeric(pr.amount))
                   as minimum_amount,
               max(convert_fraction_to_numeric(pr.amount))
                   as maximum_amount
        from (select * from payment_request where status = 'PAID' and extract(year from created_datetime) = year)
                 as pr
                 inner join "user" u on pr.id_user = u.id
        where extract(month from pr.created_datetime) = month
        group by u.first_name, pr.id_user, u.last_name;
end;
$$
    language plpgsql;