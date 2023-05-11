drop function get_billing_info_by_year_and_month(year integer, month integer);

create
or replace function get_billing_info_by_year_and_month(year integer, month integer)
    returns table
            (
                account_id     varchar,
                name           varchar,
                iban           varchar,
                bic            varchar,
                amount_value   numeric,
                average        numeric,
                median         double precision,
                minimum_amount numeric,
                maximum_amout  numeric
            )
as
$$
BEGIN
return query
select pr.account_id                                 as account_id,
       a.name                                        as name,
       a.iban                                        as iban,
       a.bic                                         as bic,
       (sum(convert_fraction_to_numeric(pr.amount))) as amount_value,
       (avg(convert_fraction_to_numeric(pr.amount))) as average,
       percentile_cont(0.5)                             WITHIN GROUP ( ORDER BY convert_fraction_to_numeric(pr.amount)) as median,
        min(convert_fraction_to_numeric(pr.amount))
                                                                                 as minimum_amount,
               max(convert_fraction_to_numeric(pr.amount))
                                                                                 as maximum_amount
from (select * from payment_request where status = 'PAID' and extract (year from created_datetime) = year)
    as pr
    inner join account a
on a.id = pr.account_id
where extract (month from pr.created_datetime) = month
group by pr.account_id, a.name, a.iban, a.bic;
end
$$
language plpgsql;